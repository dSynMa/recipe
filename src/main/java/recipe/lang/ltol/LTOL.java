package recipe.lang.ltol;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.tools.ExpressionBuilder;
import recipe.Config;
import recipe.lang.System;
import recipe.lang.agents.Agent;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.Predicate;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.*;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Integer;
import recipe.lang.utils.*;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.petitparser.parser.primitive.CharacterParser.of;


public abstract class LTOL {

    private static Pair<TypingContext, TypingContext> agentTyping(TypingContext agents){
        TypingContext agentVariables = new TypingContext();
        TypingContext transitionLabels = new TypingContext();

        for(Map.Entry<String, Type> agentVar: agents.getVarType().entrySet()){
            String name = agentVar.getKey();
            Type type = agentVar.getValue();

            Set<TypedVariable> localVars = null;
            Set<String> transitionLabelsStrings = null;
            List<Type> types = new ArrayList<>();
            if(type.getClass().equals(UnionType.class)) {
                types.addAll(((UnionType) type).getTypes());
            } else {
                types.add(type);
            }

            for(Type possibleType : types){
                Agent possibleAgent = Config.getAgent(possibleType.name());
                Set<TypedVariable> newVars = new HashSet(possibleAgent.getStore().getAttributes().values());
                if(localVars == null){
                    localVars = newVars;
                } else{
                    localVars.retainAll(newVars);
                }

                Set<String> agentspecificTransitionLabels = new HashSet<>();
                for(ProcessTransition t : possibleAgent.getSendTransitions()){
                    if(t.getLabel() != null && t.getLabel().getLabel() != null && !t.getLabel().getLabel().equals("")) {
                        agentspecificTransitionLabels.add(t.getLabel().getLabel());
                    }
                }
                for(ProcessTransition t : possibleAgent.getReceiveTransitions()){
                    if(t.getLabel() != null && t.getLabel().getLabel() != null && !t.getLabel().getLabel().equals("")) {
                        agentspecificTransitionLabels.add(t.getLabel().getLabel());
                    }
                }

                if(transitionLabelsStrings == null){
                    transitionLabelsStrings = agentspecificTransitionLabels;
                } else{
                    transitionLabelsStrings.retainAll(agentspecificTransitionLabels);
                }
            }

            agentVariables.set(name + "-automaton-state", Integer.getType());

            for(TypedVariable var : localVars) {
                agentVariables.set(name + "-" + var.getName(), var.getType());
            }

            for(String transLabel : transitionLabelsStrings){
                transitionLabels.set(name + "-" + transLabel, Boolean.getType());
            }
        }

        return new Pair<>(agentVariables, transitionLabels);
    }

    public static org.petitparser.parser.Parser parser(System system) throws Exception {
        TypingContext commonVars = new TypingContext(system.getCommunicationVariables());
        TypingContext messageVars = new TypingContext(system.getMessageStructure());

        TypingContext agents = new TypingContext();

        for(String agentTypeName : Config.getAgentTypeNames()){
            Enum type = Enum.getEnum(agentTypeName);
            for(String val : type.getValues()){
                agents.set(val, type);
            }
        }

        Pair<TypingContext, TypingContext> agentVarLabelsPair = agentTyping(agents);

        TypingContext agentVariables = agentVarLabelsPair.getLeft();
        TypingContext transitionLabels = agentVarLabelsPair.getRight();

        return parser(commonVars, messageVars, agents, agentVariables, transitionLabels, new TypingContext());
    }

    public static org.petitparser.parser.Parser parser(TypingContext commonVars, TypingContext messageVars,
                                                       TypingContext agentNames, TypingContext agentLocalVariables,
                                                       TypingContext transitionLabels, TypingContext agentVariables) throws Exception {
        ExpressionBuilder builder = new ExpressionBuilder();

        TypingContext vars = new TypingContext();
        vars.setAll(agentLocalVariables);
        vars.setAll(transitionLabels);

        // /\ v : agentKind . v != v'
        List<String> agentTypes = new ArrayList<>();
        agentTypes.addAll(Config.getAgentTypeNames());
        agentTypes.add("Agent");

        Parser agentTypeParser = Parsing.disjunctiveStringParser(agentTypes).separatedBy(StringParser.of("|").trim())
                .map((List<Object> types) -> {
                    UnionType type;
                    try {
                        Type firstType = Enum.getEnum((String) types.get(0));

                        if(types.size() == 1) return firstType;

                        type = new UnionType();
                        type.addType(firstType);

                        for(int i = 1; i < types.size(); i++){
                            if(((String) types.get(i)).contains("|")) continue;
                            String agentType = (String) types.get(i);
                            if(agentType == "Agent") {
                                return Config.getAgentType();
                            } else {
                                type.addType(Enum.getEnum(agentType));
                            }
                        }

                        return type;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });

        AtomicReference<TypingContext> newAgentVars = new AtomicReference<>(new TypingContext());

        Parser agentParametrizedStatement =
                (Parsing.typedVariableList(agentTypeParser)
                        .map((List<TypedVariable> values) -> {
                            for(TypedVariable value : values) {
                                newAgentVars.get().set(value.getName(), value.getType());
                            }
                            return values;
                        })).seq(CharacterParser.of('.').trim())
                        .seq(new LazyParser<LazyTypingContext>((LazyTypingContext context) -> {
                            TypingContext newAgentNames = new TypingContext();
                            newAgentNames.setAll(agentNames);

                            TypingContext params = context.resolve();

                            Pair<TypingContext, TypingContext> agentVarLabelsPair = agentTyping(params);

                            TypingContext newAgentVariables = agentVarLabelsPair.getLeft();
                            newAgentVariables.setAll(agentLocalVariables);
                            TypingContext newTransitionLabels = agentVarLabelsPair.getRight();
                            newTransitionLabels.setAll(transitionLabels);

                            try {
                                Parser ltolParser = LTOL.parser(commonVars, messageVars, newAgentNames, newAgentVariables,
                                        newTransitionLabels, params).trim();
                                return ltolParser;
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }, new LazyTypingContext(newAgentVars)));

        Parser bigAnd = of('/').seq(of('\\').trim())
                .seq(agentParametrizedStatement)
                .map((List<Object> vals) -> {
                    List<Object> paramsAndFormula = (List<Object>) vals.get(2);
                    return new BigAnd((List<TypedVariable>) paramsAndFormula.get(0), (LTOL) paramsAndFormula.get(2));
                });

        Parser bigOr = of('\\').seq(of('/').trim())
                .seq(agentParametrizedStatement)
                .map((List<Object> vals) -> {
                    List<Object> paramsAndFormula = (List<Object>) vals.get(2);
                    return new BigOr((List<TypedVariable>) paramsAndFormula.get(0), (LTOL) paramsAndFormula.get(2));
                });

        builder.group()
                .primitive(Condition.parser(vars).map((Condition value) -> {
                    return new Atom(value);
                })
                .or(StringParser.of("PROGRESS").map((String x)-> {return new ProgressAtom();}))
                .or(bigAnd).or(bigOr))
                .wrapper(of('(').trim(), of(')').trim(),
                        (List values) -> {
                            return values.get(1);
                        });

        Parser necessaryObs = of('<').seq(Observation.parser(commonVars, messageVars, agentVariables).trim()).seq(of('>')).trim()
                .map((List<Observation> vals) -> {
                    if(nestedQuantifiers(vals.get(1))){
                        java.util.function.Predicate<Expression<Boolean>> cond =
                                (Expression<Boolean> formula) -> {
                                    if(formula.getClass().equals(Predicate.class)){
                                        return true;
                                    } else{
                                        return false;
                                    }
                                };
                        java.util.function.Function<Expression<Boolean>, Expression<Boolean>> act =
                                (Expression<Boolean> formula) -> {
                                    if(formula.getClass().equals(Predicate.class)){
                                        String name = ((Predicate) formula).getName();
                                        Expression<Boolean> input = ((Predicate) formula).getInput();
                                        return new Predicate(name, input.removePreds());
                                    } else{
                                        return null;
                                    }
                                };
                        Observation observation = (Observation) vals.get(1);
                        Observation newObs = new Observation(observation.getObservation().replace(cond, act));
                        return newObs;
                    }
                    else{
                        return vals.get(1);
                    }
                });
        Parser sufficientObs = of('[').seq(Observation.parser(commonVars, messageVars, agentVariables).trim()).seq(of(']')).trim()
                .map((List<Observation> vals) -> {
                    if(nestedQuantifiers(vals.get(1))){
                        java.util.function.Predicate<Expression<Boolean>> cond =
                                (Expression<Boolean> formula) -> {
                                    if(formula.getClass().equals(Predicate.class)){
                                        return true;
                                    } else{
                                        return false;
                                    }
                                };
                        java.util.function.Function<Expression<Boolean>, Expression<Boolean>> act =
                                (Expression<Boolean> formula) -> {
                                    if(formula.getClass().equals(Predicate.class)){
                                        String name = ((Predicate) formula).getName();
                                        Expression<Boolean> input = ((Predicate) formula).getInput();
                                        return new Predicate(name, input.removePreds());
                                    } else{
                                        return null;
                                    }
                                };
                        Observation observation = (Observation) vals.get(1);
                        Observation newObs = new Observation(observation.getObservation().replace(cond, act));
                        return newObs;
                    }
                    else{
                        return vals.get(1);
                    }
                });

        builder.group()
                .prefix(of('!').trim(), (List<LTOL> values) -> new Not(values.get(1)))
                .prefix(of('G').trim(), (List<LTOL> values) -> new Globally(values.get(1)))
                .prefix(of('F').trim(), (List<LTOL> values) -> {
                    return new Eventually(values.get(1));
                })
                .prefix(of('X').trim(), (List<LTOL> values) -> new Next(values.get(1)))
                .prefix(sufficientObs.trim(), (List<Object> values) -> new Possibly((Observation) values.get(0), (LTOL) values.get(1)))
                .prefix(necessaryObs.trim(), (List<Object> values) -> {
                    return new Necessary((Observation) values.get(0), (LTOL) values.get(1));
                });


        builder.group()
                .left(StringParser.of("U").trim(), (List<LTOL> values) -> new Until(values.get(0), values.get(2)))
                .left(StringParser.of("W").trim(), (List<LTOL> values) -> new Or(new Globally(values.get(0)), new Until(values.get(0), values.get(2))))
                .left(StringParser.of("R").trim(), (List<LTOL> values) -> new Until(new Not(values.get(0)), new Not(values.get(2))));


        builder.group()
                .right(StringParser.of("->").trim(), (List<LTOL> values) -> new Or(new Not(values.get(0)), values.get(2)))
                .right(StringParser.of("<->").or(StringParser.of("=").plus()).trim(), (List<LTOL> values) -> {
                    try {
                        return new Iff(values.get(0), values.get(2));
                    } catch (Exception e) {
                        return e;
                    }
                })
                .left(StringParser.of("<->").or(StringParser.of("=").plus()).trim(), (List<LTOL> values) -> {
                    try {
                        return new Iff(values.get(0), values.get(2));
                    } catch (Exception e) {
                        return e;
                    }
                })
                .right(StringParser.of("!=").trim(), (List<LTOL> values) -> {
                    return new Not(new Iff(values.get(0), values.get(2)));
                })
                .left(StringParser.of("!=").trim(), (List<LTOL> values) -> {
                    return new Not(new Iff(values.get(0), values.get(2)));
                });

        builder.group()
                .right(of('&').plus().trim(), (List<LTOL> values) -> new And(values.get(0), values.get(2)))
                .left(of('&').plus().trim(), (List<LTOL> values) -> new And(values.get(0), values.get(2)))
                .right(of('|').plus().trim(), (List<LTOL> values) -> new Or(values.get(0), values.get(2)))
                .left(of('|').plus().trim(), (List<LTOL> values) -> new Or(values.get(0), values.get(2)));

        return builder.build();
    }

    public abstract boolean isPureLTL();

    public abstract Triple<java.lang.Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) throws Exception;
    @Override
    public boolean equals(Object expr){
        return this.toString().equals(expr.toString());
    }
    @Override
    public int hashCode(){
        return this.toString().hashCode();
    }

    public abstract LTOL rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException;

    public abstract LTOL toLTOLwithoutQuantifiers() throws Exception;

    protected static Set<LTOL> rewriteOutBigAndOr(List<TypedVariable> vars, LTOL ltol) throws Exception {
        Set<LTOL> possibleValues = new HashSet<>();
        possibleValues.add(ltol);
        for (TypedVariable var : vars) {
            Set<TypedVariable> possibleReferences = new HashSet<>();
            Set<TypedValue> possibleAgentInstances = var.getType().getAllValues();
            Set<String> possibleAgentInstancesNames = new HashSet<>();
            Set<Agent> possibleAgentTypes = new HashSet<>();
            Map<String, Type> agentInstanceNameToType = new HashMap<>();

            for (Object concreteVar : possibleAgentInstances) {
                TypedValue value = (TypedValue) concreteVar;
                if (value.getValue().equals(Config.noAgentString)) continue;
                possibleAgentInstancesNames.add((String) value.getValue());
                possibleAgentTypes.add(Config.getAgent(value.getType().name()));
                agentInstanceNameToType.put((String) value.getValue(), value.getType());
            }

            boolean start = true;
            for (Agent agent : possibleAgentTypes) {
                if (start) {
                    possibleReferences.addAll(agent.getStore().getAttributes().values());
                    possibleReferences.addAll(agent.getAllTransitionLabels());
                } else {
                    possibleReferences.retainAll(agent.getStore().getAttributes().values());
                    possibleReferences.retainAll(agent.getAllTransitionLabels());
                }
            }

            Set<TypedVariable> labelledPossibleReferences = new HashSet<>();

            for(TypedVariable ref : possibleReferences){
                labelledPossibleReferences.add(new TypedVariable(ref.getType(), var.getName() + "-" + ref.getName()));
            }

            Set<LTOL> newPossibleValues = new HashSet<>();

            for(LTOL ltol1 : possibleValues){
                for(String agentName : possibleAgentInstancesNames){
                    LTOL ltol2 = ltol1.rename((x) -> containsQualified(labelledPossibleReferences, possibleAgentTypes, x) ? new TypedVariable(x.getType(), x.toString().replaceAll("^" + var.toString(), agentName)) : x);
                    Type agentType = agentInstanceNameToType.get(agentName);
                    for(TypedVariable agentVar : vars) {
                        ltol2 = ltol2.rename((x) -> x.getName().equals(agentVar.getName()) ? new TypedVariable(agentType, agentName) : x);
                        newPossibleValues.add(ltol2);
                    }
                }
            }

            possibleValues = newPossibleValues;
        }

        return possibleValues;
    }

    private static boolean containsQualified(Set<TypedVariable> set, Set<Agent> possibleAgentTypes, TypedVariable x) {
        String unqualifiedName = x.getName().split("-")[1];
        // Find a matching local variable and set x's type to that
        for (Agent a : possibleAgentTypes) {
            for (TypedVariable tv : a.getStore().getAttributes().values()) {
                if (tv.getName().equals(unqualifiedName)) {
                    TypedVariable<Type> xWithType = new TypedVariable<>(tv.getType(), x.getName());
                    // Now check for inclusion
                    for (TypedVariable typedVariable : set) {
                        if (typedVariable.equals(xWithType)) return true;
                    }
                }
            }
        }
        // Nothing was found :(
        return false;
    }

    private static java.lang.Boolean nestedQuantifiers(Observation observation){
        Expression<Boolean> condition = observation.observation;
        Set<Expression<recipe.lang.types.Boolean>> subformulas = condition.subformulas();
        for(Expression<recipe.lang.types.Boolean> expr : subformulas){
            if(expr.getClass().equals(Predicate.class)) {
                Expression<recipe.lang.types.Boolean> predExpr = ((Predicate) expr).getInput();
                for (Expression<recipe.lang.types.Boolean> exprr : predExpr.subformulas()) {
                    if (exprr.getClass().equals(Predicate.class)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
