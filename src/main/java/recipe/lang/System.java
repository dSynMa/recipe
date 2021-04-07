package recipe.lang;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import recipe.lang.agents.Agent;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.LazyParser;
import recipe.lang.utils.Pair;
import recipe.lang.utils.Triple;
import recipe.lang.utils.TypingContext;

import java.util.*;
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

        AtomicReference<TypingContext> channelContext = new AtomicReference<>(new TypingContext());
        AtomicReference<TypingContext> messageContext = new AtomicReference<>(new TypingContext());
        AtomicReference<TypingContext> communicationContext = new AtomicReference<>(new TypingContext());
        AtomicReference<Pair> guardDefinitionsContext = new AtomicReference<>();

        parser.set((labelledParser("channels", channelValues())
                        .map((List<ChannelValue> values) -> {
                            for(ChannelValue v : values){
                                channelContext.get().set(v.getValue(), v);
                            }
                            return values;
                        }))
                .seq(labelledParser("message-structure", typedVariableList())
                        .map((Map<String, Expression> values) -> {
                            messageContext.get().setAll(new TypingContext(values));
                            return values;
                        }))
                .seq(labelledParser("communication-variables", typedVariableList())
                        .map((Map<String, Expression> values) -> {
                            communicationContext.get().setAll(new TypingContext(values));
                            return values;
                        }))
                .seq(guardDefinitionList()
                        .map((Pair values) -> {
                            guardDefinitionsContext.set(values);
                            return values;
                        }))
                .seq(new LazyParser<>(
                        (Triple<TypingContext, TypingContext, TypingContext> msgCmncChnContext) ->
                                Agent.parser(msgCmncChnContext.getLeft(),
                                        msgCmncChnContext.getMiddle(),
                                        msgCmncChnContext.getRight()).plus(),
                        new Triple(messageContext.get(), communicationContext.get(), channelContext.get())))
                .map((List<Object> values) -> {
                    Set<String> channels = new HashSet<>((List<String>) values.get(0));
                    Map<String, TypedVariable> messageStructure = (Map<String, TypedVariable>) values.get(1);
                    Map<String, TypedVariable> communicationVariables = (Map<String, TypedVariable>) values.get(2);
                    Pair guardDefinitions = (Pair) values.get(3);

                    Set<Agent> agents = new HashSet<>((List<Agent>) values.get(4));

                    return new System(channels, messageStructure, communicationVariables, agents);
                })
        );

        return parser;
    }
}
