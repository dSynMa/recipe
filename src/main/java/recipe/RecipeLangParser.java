package recipe;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.parser.combinators.SettableParser;
import recipe.lang.actions.ReceiveAction;
import recipe.lang.actions.SendAction;
import recipe.lang.agents.AgentBehaviour;
import recipe.lang.agents.Choice;
import recipe.lang.agents.Guarded;
import recipe.lang.agents.Sequence;
import recipe.lang.conditions.*;

import java.util.List;

import static org.petitparser.parser.primitive.CharacterParser.*;

public class RecipeLangParser {
    SettableParser action;
    SettableParser condition;
    SettableParser agentBehaviour;

    private void initConditionParser(){
        condition = SettableParser.undefined();
        SettableParser bracketedCondition = SettableParser.undefined();

        bracketedCondition.set(StringParser.of("(").trim().seq(condition).seq(StringParser.of(")").trim())
                        .map((List<Object> values) -> {
                            Condition cond = (Condition) values.get(1);
                            return cond;
                        })
        );

        SettableParser basicCondition = SettableParser.undefined();

        basicCondition.set((CharacterParser.of('!').trim()).seq(basicCondition)
                .map((List<Object> values) -> {
                    Condition cond = (Condition) values.get(1);
                    return new Not(cond);
                })
                .or(StringParser.of("(").trim().seq(condition).seq(StringParser.of(")").trim())
                        .map((List<Object> values) -> {
                            Condition cond = (Condition) values.get(1);
                            return cond;
                        }))
                .or(word().plus().flatten()
                        .map((String value) -> {
                            return new Atom(value);
                        }))
        );

        condition.set((basicCondition.seq(CharacterParser.of('&').trim()).seq(basicCondition)
                .map((List<Object> values) -> {
                    Condition cond1 = (Condition) values.get(0);
                    Condition cond2 = (Condition) values.get(2);
                    return new And(cond1, cond2);
                }))
                .or(basicCondition.seq(StringParser.of("|").trim()).seq(basicCondition)
                        .map((List<Object> values) -> {
                            Condition cond1 = (Condition) values.get(0);
                            Condition cond2 = (Condition) values.get(2);
                            return new Or(cond1, cond2);
                        }))
                .or(basicCondition)
        );
    }

    public boolean parseCondition(String s){
        Parser start = condition.end();

        return start.accept(s);
    }

    private void initActionParser(){
        Parser equation = word().seq(StringParser.of("=").trim()).seq(word());
        action = SettableParser.undefined();
        SettableParser channelParser = SettableParser.undefined();

        channelParser.set(condition
                .seq(CharacterParser.of('#').trim())
                .seq(word().plus().trim()) //ch variable
                .seq(CharacterParser.of('#').trim()));

        // cond#receiver#!(v,w)[x = v, y = w]>(cond)
        SettableParser sendActionParser = SettableParser.undefined();

        sendActionParser.set(channelParser
                .seq(CharacterParser.of('!'))
                .seq((CharacterParser.of('(').trim()))
                .seq((((word().seq(StringParser.of(",").trim()))).star().seq(word().trim())).or(word()))
                .seq((CharacterParser.of(')').trim()))
                .seq((CharacterParser.of('[').trim()))
                .seq(((equation.seq(CharacterParser.of(',').trim())).star().seq(equation.trim())).or(equation))
                .seq((CharacterParser.of(']').trim()))
                .seq((CharacterParser.of('>').trim()))
                .seq(condition)
                .map((List<Object> values) -> {
                    Condition psi = (Condition) values.get(0);
                    String channel = (String) values.get(2);
                    String message = (String) values.get(6);
                    String update = (String) values.get(9);
                    Condition guard = (Condition) values.get(12);
                    SendAction action = new SendAction(psi, channel, message, update, guard);
                    return action;
                }));

        SettableParser receiveActionParser = SettableParser.undefined();

        receiveActionParser.set(channelParser
                .seq(CharacterParser.of('?'))
                .seq((CharacterParser.of('(').trim()))
                .seq((((word().seq(StringParser.of(",").trim()))).star().seq(word().trim())).or(word()))
                .seq((CharacterParser.of(')').trim()))
                .seq((CharacterParser.of('[').trim()))
                .seq(((equation.seq(CharacterParser.of(',').trim())).star().seq(equation.trim())).or(equation))
                .seq((CharacterParser.of(']').trim()))
                .map((List<Object> values) -> {
                    Condition psi = (Condition) values.get(0);
                    String channel = (String) values.get(2);
                    String message = (String) values.get(6);
                    String update = (String) values.get(9);
                    ReceiveAction action = new ReceiveAction(psi, channel, message, update);
                    return action;
                }));

        action.set(sendActionParser.or(receiveActionParser));
    }

    public boolean parseAction(String s){
        Parser start = action.end();

        return start.accept(s);
    }

    public void initAgentBehaviourParser(){
        agentBehaviour = SettableParser.undefined();
        SettableParser basic = SettableParser.undefined();

        basic.set((CharacterParser.of('(').trim()).seq(action).seq((CharacterParser.of(')').trim()))
                    .map((List<Object> values) -> {
                            return (SendAction) values.get(0);
                    }));
        
        agentBehaviour.set((basic.seq(StringParser.of("+").trim()).seq(agentBehaviour))
                            .map((List<Object> values) -> {
                                return new Choice((AgentBehaviour) values.get(0), (AgentBehaviour) values.get(2));
                            })
                .or(basic.seq(StringParser.of(";").trim()).seq(agentBehaviour)
                        .map((List<Object> values) -> {
                            return new Sequence((AgentBehaviour) values.get(0), (AgentBehaviour) values.get(2));
                        }))
                .or((StringParser.of("<").trim().seq(condition).seq(StringParser.of(">").trim())).seq(agentBehaviour)
                        .map((List<Object> values) -> {
                            return new Guarded((Condition) values.get(1), (AgentBehaviour) values.get(3));
                        }))
                .or(basic.seq(agentBehaviour)
                        .map((List<Object> values) -> {
                            return (SendAction) values.get(0);
                        }))
                .or(action));
    }

    public boolean parseAgentBehaviour(String s){
        Parser start = agentBehaviour.end();

        return start.accept(s);
    }

    public RecipeLangParser() {
        initConditionParser();
        initActionParser();
        initAgentBehaviourParser();
    }
}

