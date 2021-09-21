package recipe.lang;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.exception.ParsingException;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.types.Enum;
import recipe.lang.types.Guard;
import recipe.lang.types.Type;
import recipe.lang.utils.LazyParser;
import recipe.lang.utils.Pair;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static recipe.lang.utils.Parsing.*;
import static recipe.lang.utils.Parsing.typedVariableList;

public class System{
    Map<String, Type> messageStructure;
    Map<String, Type> communicationVariables;
    Map<String, Type> guardDefinitions;
    Set<Agent> agents;
    Set<AgentInstance> agentsInstances;

    public List<String> getLtlspec() {
        return specs;
    }

    List<String> specs;

    public Map<String, Type> getMessageStructure() {
        return messageStructure;
    }

    public Map<String, Type> getCommunicationVariables() {
        return communicationVariables;
    }

    public Set<Agent> getAgents() {
        return agents;
    }
    public Set<AgentInstance> getAgentInstances() {
        return agentsInstances;
    }

    public System(Map<String, Type> messageStructure, Map<String, Type> communicationVariables, Map<String, Type> guardDefinitions, Set<Agent> agents, Set<AgentInstance> agentsInstances, List<String> specs) {
        this.messageStructure = messageStructure;
        this.communicationVariables = communicationVariables;
        this.guardDefinitions = guardDefinitions;
        this.agents = agents;
        this.agentsInstances = agentsInstances;
        this.specs = specs;
    }

    public static Parser parser(){
        Enum.clear();
        Guard.clear();

        SettableParser parser = SettableParser.undefined();

//        AtomicReference<TypingContext> channelContext = new AtomicReference<>(new TypingContext());
        AtomicReference<TypingContext> messageContext = new AtomicReference<>(new TypingContext());
        AtomicReference<TypingContext> communicationContext = new AtomicReference<>(new TypingContext());
        AtomicReference<TypingContext> guardDefinitionsContext = new AtomicReference<>(new TypingContext());
        AtomicReference<Set<Agent>> agents = new AtomicReference<>(new HashSet<>());

        parser.set((labelledParser("channels", channelValues())
                        .mapWithSideEffects((List<String> values) -> {
                            try {
                                if (values.contains(Config.broadcast)) {
                                    throw new ParsingException(Config.broadcast + " is a reserved keyword and defined implicit, there is no need to add it to declared channel values.");
                                }
                                //Do not remove this, enum stored in Enum.existing
                                new Enum(Config.channelWithoutBroadcastLabel, values);

                                List<String> valuesWithBroadcast = new ArrayList<>(values);
                                valuesWithBroadcast.add(Config.broadcast);
                                Enum channelEnum = new Enum(Config.channelLabel, valuesWithBroadcast);
//                                for(String v : valuesWithBroadcast){
//                                    channelContext.get().set(v, channelEnum);
//                                }
                            } catch (TypeCreationException | ParsingException e) {
                                e.printStackTrace();
                            }
                            return values;
                        }))
                        .seq(Parsing.enumDefinitionParser().star())
                        .seq(labelledParser("message-structure", typedVariableList())
                                .map((Map<String, Type> values) -> {
                                    messageContext.get().setAll(new TypingContext(values));
                                    return values;
                                }))
                        .seq(labelledParser("communication-variables", typedVariableList())
                                .map((Map<String, Type> values) -> {
                                    communicationContext.get().setAll(new TypingContext(values));
                                    return values;
                                }))
                        .seq(guardDefinitionList(new TypingContext()) //TODO may want to range over channel values and communication values in future
                                .map((Map<String, Type> values) -> {
                                    guardDefinitionsContext.get().setAll(new TypingContext(values));
                                    return values;
                                }).optional())
                        .seq(new LazyParser<>(
                                (Pair<Pair<TypingContext, TypingContext>, TypingContext> msgCmncGuardContext) ->
                                {
                                    try {
                                        Parser agent = Agent.parser(msgCmncGuardContext.getLeft().getLeft(),
                                                msgCmncGuardContext.getLeft().getRight(), msgCmncGuardContext.getRight()).plus();
                                        return agent;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                },
                                new Pair<>(new Pair<>(messageContext.get(), communicationContext.get()), guardDefinitionsContext.get()))
                                .map((List<Agent> agentss) -> {
                                    agents.get().addAll(agentss);
                                    return agentss;
                                })
                        )
                        .seq(labelledParser("system", "=", new LazyParser<Boolean>((Boolean b) -> {
                            return AgentInstance.parser(agents.get()).separatedBy(CharacterParser.of('|'));
                        }, true))
                                        .map((List<Object> values) -> {
                                            List<AgentInstance> agentInstances = new ArrayList<>();
                                            for (Object x : values) {
                                                if(x.getClass().equals(AgentInstance.class)){
                                                    agentInstances.add((AgentInstance) x);
                                                }
                                            }

                                            return agentInstances;
                                        })
                        )
                        .seq(((StringParser.of("LTLSPEC")
                                .or(StringParser.of("CTLSPEC"))
                                .or(StringParser.of("INVARSPEC"))
                                .or(StringParser.of("PSLSPEC"))
                                .or(StringParser.of("COMPUTE"))
                                .or(StringParser.of("SPEC"))
                                .or(StringParser.of("PARSYNTH"))).flatten().seq(CharacterParser.any().plus()).flatten()).delimitedBy(CharacterParser.of(';')).optional(new ArrayList<>()))
                        .map((List<Object> values) -> {
                            Set<String> channels = new HashSet<>((List<String>) values.get(0));
                            Map<String, Type> messageStructure = (Map<String, Type>) values.get(2);
                            Map<String, Type> communicationVariables = (Map<String, Type>) values.get(3);
                            Map<String, Type> guardDefinitions = (Map<String, Type>) values.get(4);
                            Set<AgentInstance> agentInstances = new HashSet<>((List) values.get(6));

                            List<String> specs = (List<String>) values.get(7);

                            return new System(messageStructure, communicationVariables, guardDefinitions, agents.get(), agentInstances, specs);
                        })
        );

        return parser;
    }

    public List<String> toDOT(){
        List<String> dot = new ArrayList<String>();
        for(AgentInstance agentInstance : this.agentsInstances){
            Agent agent = agentInstance.getAgent();
            String digraph = "digraph \"" + agentInstance.getLabel() + "\"{\n" + agent.toDOT() + "\n}";
            digraph = digraph.replaceAll("\\\"", "\\\\\"");
            digraph = digraph.replaceAll("\n\t", " ");
            digraph = digraph.replaceAll("\n+", " ");
            digraph = "{\"name\" : \"" + agent.getName() + "\", \"graph\" : \"" + digraph + "\"}";
            dot.add(digraph);
        }

        return dot;
    }
}
