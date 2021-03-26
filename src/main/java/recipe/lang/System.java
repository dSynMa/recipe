package recipe.lang;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import recipe.lang.agents.Agent;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.utils.Pair;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static recipe.lang.utils.Parsing.*;
import static recipe.lang.utils.Parsing.typedVariableList;

public class System{
    Set<String> channels;
    Map<String, TypedVariable> messageStructure;
    Map<String, TypedVariable> communicationVariables;
    Set<Agent> agents;

    public Set<String> getChannels() {
        return channels;
    }

    public Map<String, TypedVariable> getMessageStructure() {
        return messageStructure;
    }

    public Map<String, TypedVariable> getCommunicationVariables() {
        return communicationVariables;
    }

    public Set<Agent> getAgents() {
        return agents;
    }


    public System(Set<String> channels, Map<String, TypedVariable> messageStructure, Map<String, TypedVariable> communicationVariables, Set<Agent> agents) {
        this.channels = channels;
        this.messageStructure = messageStructure;
        this.communicationVariables = communicationVariables;
        this.agents = agents;
    }

    public static Parser parser(){
        SettableParser parser = SettableParser.undefined();

        AtomicReference<TypingContext> channelContext = new AtomicReference<>();
        AtomicReference<TypingContext> messageContext = new AtomicReference<>();
        AtomicReference<TypingContext> communicationContext = new AtomicReference<>();
        AtomicReference<TypingContext> guardDefinitionsContext = new AtomicReference<>();

        parser.set((labelledParser("channels", channelValues())
                        .map((Map<String, Expression> values) -> {
                            channelContext.set(new TypingContext(values));
                            return values;
                        }))
                .seq(labelledParser("message-structure", typedVariableList())
                        .map((Map<String, Expression> values) -> {
                            messageContext.set(new TypingContext(values));
                            return values;
                        }))
                .seq(labelledParser("communication-variables", typedVariableList())
                        .map((Map<String, Expression> values) -> {
                            communicationContext.set(new TypingContext(values));
                            return values;
                        }))
                .seq(guardDefinitionList()
                        .map((Map<String, Expression> values) -> {
                            guardDefinitionsContext.set(new TypingContext(values));
                            return values;
                        }))
                .seq(Agent.parser(messageContext.get(), communicationContext.get(), channelContext.get()))
                .map((List<Object> values) -> {
                    //TODO collect context (channels, message structue, communication vars)
                    Set<String> channels = (Set<String>) values.get(0);
                    Map<String, TypedVariable> messageStructure = (Map<String, TypedVariable>) values.get(1);
                    Map<String, TypedVariable> communicationVariables = (Map<String, TypedVariable>) values.get(2);
                    Map<String, Pair> guardDefinitions = (Map<String, Pair>) values.get(3);

                    Set<Agent> agents = (Set<Agent>) values.get(3);

                    return new System(channels, messageStructure, communicationVariables, agents);
                })
        );

        return parser;
    }
}
