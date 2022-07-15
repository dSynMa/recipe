package recipe.lang.ltol;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.tools.ExpressionBuilder;
import recipe.Config;
import recipe.lang.System;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Integer;
import recipe.lang.utils.Triple;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Map;

import static org.petitparser.parser.primitive.CharacterParser.of;

//TODO we will be needing to 'compose' this with the system, such that we have a new variable for each top-level observation
// so if we have <forall>(o || o'), we will have a variable for o || o'.
// note: observations about cvs, channel being sent on, agent names, and data variables
// 1. unquantified observations can just be evaluated over

public abstract class LTOL {
    public static org.petitparser.parser.Parser parser(System system) throws Exception {
        ExpressionBuilder builder = new ExpressionBuilder();

        TypingContext commonVars = new TypingContext(system.getCommunicationVariables());
        TypingContext messageVars = new TypingContext(system.getMessageStructure());
        TypingContext agentNames = new TypingContext();

        Enum agentType;
        if(!Enum.exists(Config.agentEnumType)) {
            agentType = new Enum(Config.agentEnumType,
                    system.getAgentInstances()
                            .stream()
                            .map(AgentInstance::getLabel)
                            .toList());
        } else{
            agentType = Enum.getEnum(Config.agentEnumType);
        }

        for(AgentInstance agentInstance : system.getAgentInstances()){
            agentNames.set(agentInstance.getLabel(), agentType);
        }

        TypingContext localVariables = new TypingContext();
        TypingContext transitionLabels = new TypingContext();

        for(AgentInstance agentInstance : system.getAgentInstances()){
            String name = agentInstance.getLabel();
            localVariables.set(name + "-state", Integer.getType());

            for(Map.Entry<String, TypedVariable> entry : agentInstance.getAgent().getStore().getAttributes().entrySet()) {
                localVariables.set(name + "-" + entry.getKey(), entry.getValue().getType());
            }

            for(ProcessTransition t : agentInstance.getAgent().getSendTransitions()){
                if(t.getLabel() != null && t.getLabel().getLabel() != null && !t.getLabel().getLabel().equals("")) {
                    transitionLabels.set(name + "-" + t.getLabel().getLabel(), Boolean.getType());
                }
            }

            for(ProcessTransition t : agentInstance.getAgent().getReceiveTransitions()){
                if(t.getLabel() != null && t.getLabel().getLabel() != null && !t.getLabel().getLabel().equals("")) {
                    transitionLabels.set(name + "-" + t.getLabel().getLabel(), Boolean.getType());
                }
            }
        }

        TypingContext vars = new TypingContext();
        vars.setAll(localVariables);
        vars.setAll(transitionLabels);

        builder.group()
                .primitive(Condition.parser(vars).map((Condition value) -> {
                    return new Atom(value);
                }))
                .wrapper(of('(').trim(), of(')').trim(),
                        (List values) -> {
                    return values.get(1);
                        });

        // negation is a prefix
        builder.group()
                .prefix(of('!').trim(), (List<LTOL> values) -> new Not(values.get(1)));

        builder.group()
                .prefix(of('G').trim(), (List<LTOL> values) -> new Globally(values.get(1)));

        builder.group()
                .prefix(of('F').trim(), (List<LTOL> values) -> new Eventually(values.get(1)));

        builder.group()
                .prefix(of('X').trim(), (List<LTOL> values) -> new Next(values.get(1)));

        // conjunction is right- and left-associative
        builder.group()
                .right(of('&').plus().trim(), (List<LTOL> values) -> new And(values.get(0), values.get(2)))
                .left(of('&').plus().trim(), (List<LTOL> values) -> new And(values.get(0), values.get(2)));

        builder.group()
                .right(StringParser.of("->").trim(), (List<LTOL> values) -> new Or(new Not(values.get(0)), values.get(2)));

        builder.group()
                .left(StringParser.of("U").trim(), (List<LTOL> values) -> new Until(values.get(0), values.get(2)));

        builder.group()
                .left(StringParser.of("W").trim(), (List<LTOL> values) -> new Or(new Globally(values.get(0)), new Until(values.get(0), values.get(2))));

        builder.group()
                .left(StringParser.of("R").trim(), (List<LTOL> values) -> new Until(new Not(values.get(0)), new Not(values.get(2))));


        // disjunction is right- and left-associative
        builder.group()
                .right(of('|').plus().trim(), (List<LTOL> values) -> new Or(values.get(0), values.get(2)))
                .left(of('|').plus().trim(), (List<LTOL> values) -> new Or(values.get(0), values.get(2)));

        Parser necessaryObs = of('<').seq(Observation.parser(commonVars, messageVars, agentNames).trim()).seq(of('>'))
                .map((List<Observation> vals) -> {
                    return vals.get(1);
                });
        Parser sufficientObs = of('[').seq(Observation.parser(commonVars, messageVars, agentNames).trim()).seq(of(']'))
                .map((List<Observation> vals) -> {
                    return vals.get(1);
                });

        builder.group()
                .prefix(necessaryObs, (List<Object> values) -> {
                    return new Necessary((Observation) values.get(0), (LTOL) values.get(1));
                });

        builder.group()
                .prefix(sufficientObs, (List<Object> values) -> new Possibly((Observation) values.get(0), (LTOL) values.get(1)));

        // /\ v : agentKind . v != v'
//        Parser bigAnd = of('/').seq(of('\\').trim()).seq(Parsing.)
//                .seq(Observation.parser(commonVars, messageVars).trim()).seq(of('>')).map((List<Observation> vals) -> vals.get(1));

//        // implication is right-associative
//        builder.group()
//                .right(StringParser.of("->").plus().trim(), (List<LTOL> values) -> new Implies(values.get(0), values.get(2)));

//        // iff is left and right-associative
//        builder.group()
//                .right(StringParser.of("<->").or(StringParser.of("=").plus()).trim(), (List<LTOL> values) -> {
//                    try {
//                        return new IsEqualTo(values.get(0), values.get(2));
//                    } catch (Exception e) {
//                        return e;
//                    }
//                })
//                .left(StringParser.of("<->").or(StringParser.of("=").plus()).trim(), (List<Condition> values) -> {
//                    try {
//                        return new IsEqualTo(values.get(0), values.get(2));
//                    } catch (Exception e) {
//                        return e;
//                    }
//                });

//        // is not equal to is left and right-associative
//        builder.group()
//                .right(StringParser.of("!=").trim(), (List<Condition> values) -> {
//                    return new IsNotEqualTo(values.get(0), values.get(2));
//                })
//                .left(StringParser.of("!=").trim(), (List<Condition> values) -> {
//                    return new IsNotEqualTo(values.get(0), values.get(2));
//                });


        return builder.build();
    }

    public abstract boolean isPureLTL();

    public abstract Triple<java.lang.Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter);
    @Override
    public boolean equals(Object expr){
        return this.toString().equals(expr.toString());
    }
    @Override
    public int hashCode(){
        return this.toString().hashCode();
    }
}
