package recipe.analysis;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import recipe.Config;
import recipe.lang.System;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.Predicate;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.Implies;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.expressions.predicate.IsNotEqualTo;
import recipe.lang.expressions.predicate.Not;
import recipe.lang.expressions.predicate.Or;
import recipe.lang.ltol.LTOL;
import recipe.lang.ltol.Observation;
import recipe.lang.types.Boolean;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Enum;
import recipe.lang.types.Type;
import recipe.lang.utils.Pair;
import recipe.lang.utils.Triple;
import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;
import recipe.lang.utils.exceptions.TypeCreationException;

public class ToNuXmv {

    public static Expression<Boolean> specialiseObservationToSendTransition(Map<String, Type> cvs,
                                                                            Expression<Boolean> obs,
                                                                            Expression<Boolean> sendGuard,
                                                                            Map<String, ? extends Expression<?>> message,
                                                                            TypedValue<?> sender,
                                                                            Expression<?> channel,
                                                                            Set<String> messageFields) throws Exception {
        AtomicReference<java.lang.Boolean> missingField = new AtomicReference<>(false);
        obs.relabel(v -> {
            if (messageFields.contains(v.getName()) && !message.containsKey(v.getName())) missingField.set(true);
            return v;
        });
        // Match interpreter evaluation: an observation requiring absent data is not satisfied.
        if (missingField.get()) return Condition.getFalse();
        //handle message and sender variables
        Expression<Boolean> observation = obs.relabel((v) -> message.containsKey(v.getName()) ? message.get(v.getName()) : v)
                                                .relabel((v) -> v.getName().equals("sender") ? sender : v)
                                                .relabel((v) -> v.getName().equals(Config.channelLabel) ? channel : v);
        observation = observation.simplify();
        return handleCVsInObservation(cvs, observation, sendGuard).simplify();
    }

    public static Set<Expression<Boolean>> specialiseOnAllPossibleValues(String cv,
                                                                         Type type,
                                                                         Expression<Boolean> cond
                                                                    ) throws InfiniteValueTypeException,
                                                                            RelabellingTypeException,
                                                                            MismatchingTypeException,
                                                                            AttributeTypeException,
                                                                            TypeCreationException,
                                                                            AttributeNotInStoreException {
        //associate cv with all its possible values
        //instantiate condition for each value

        //get all cvs and order them
        List<TypedValue<?>> vals = new ArrayList<>();
        if(type.getClass().equals(Boolean.class)){
            vals.add(Condition.getTrue());
            vals.add(Condition.getFalse());
        } else if(type.getClass().equals(BoundedInteger.class)){
            for (TypedValue<?> value : ((BoundedInteger) type).getAllValues()) vals.add(value);
        } else if(type.getClass().equals(Enum.class)){
            for (TypedValue<?> value : ((Enum) type).getAllValues()) vals.add(value);
        } else {
            throw new InfiniteValueTypeException("For LTOL analysis communication variables must be of finite type.");
        }

        Set<Expression<Boolean>> conditions = new HashSet<>();
        final String cv_cleaned = cv.replaceAll("^@", "");
        for(int j = 0; j < vals.size(); j++){
            TypedValue<?> val = vals.get(j);
            conditions.add(cond.relabel((v) -> {
                String v_cleaned = v.getName().replaceAll("^@", "");
                if(v_cleaned.equals(cv_cleaned)){
                    return val;
                } else{
                    return v;
                }
            }).simplify());
        }

        return conditions;
    }

    public static Expression<Boolean> handleCVsInObservation(Map<String, Type> cvs, Expression<Boolean> obs, Expression<Boolean> sendGuard) throws Exception {
        // ReCiPe section 7: https://arxiv.org/html/2104.10998v3#S7
        // m |= cv iff every valuation in the sender predicate satisfies cv.
        // Negation stays outside: m |= !cv iff some selected valuation falsifies cv.
        // These are valuations of common variables, not the actual connected receivers.
        if (obs instanceof TypedVariable && obs.getType().equals(Boolean.getType())) {
            String name = ((TypedVariable) obs).getName().replaceFirst("^@", "");
            if (cvs.containsKey(name) || cvs.containsKey("@" + name)) {
                return handleCVsInObservation(cvs, new Predicate("forall", obs), sendGuard);
            }
        }
        if(obs.getClass().equals(And.class)){
            And obss = (And) obs;
            return new And(handleCVsInObservation(cvs, obss.getLhs(), sendGuard), handleCVsInObservation(cvs, obss.getRhs(), sendGuard));
        } else if(obs.getClass().equals(Or.class)){
            Or obss = (Or) obs;
            return new Or(handleCVsInObservation(cvs, obss.getLhs(), sendGuard), handleCVsInObservation(cvs, obss.getRhs(), sendGuard));
        } else if(obs.getClass().equals(Not.class)){
            Not obss = (Not) obs;
            return new Not(handleCVsInObservation(cvs, obss.getArgument(), sendGuard));
        } else if(obs.getClass().equals(Implies.class)){
            Implies obss = (Implies) obs;
            return new Implies(handleCVsInObservation(cvs, obss.getLhs(), sendGuard), handleCVsInObservation(cvs, obss.getRhs(), sendGuard));
        } else if(obs.getClass().equals(IsEqualTo.class)){
            IsEqualTo obss = (IsEqualTo) obs;
            if(obss.getLhs().getType().equals(Boolean.getType())){
                return new And(
                        new Implies(handleCVsInObservation(cvs, obss.getLhs(), sendGuard), handleCVsInObservation(cvs, obss.getRhs(), sendGuard)),
                        new Implies(handleCVsInObservation(cvs, obss.getRhs(), sendGuard), handleCVsInObservation(cvs, obss.getLhs(), sendGuard)));
            } else {
                return specialiseAtomicObservation(cvs, obs, sendGuard);
            }
        } else if (obs instanceof IsNotEqualTo) {
            IsNotEqualTo inequality = (IsNotEqualTo) obs;
            return new Not(handleCVsInObservation(cvs,
                new IsEqualTo(inequality.getLhs(), inequality.getRhs()), sendGuard));
        } else if(obs.getClass().equals(Predicate.class)){
            Predicate obss = (Predicate) obs;
            //t(forall(o)) = /\cv g_s -> o

            if(obss.getName().equals("forall")){
                Expression<Boolean> finalExpression = null;

                Set<Expression<Boolean>> current = new HashSet<>();
                current.add(new Implies(sendGuard, obss.getInput()));

                for(Map.Entry<String, Type> entry : cvs.entrySet()){
                    Set<Expression<Boolean>> next = new HashSet<>();
                    for(Expression<Boolean> expr : current){
                        Set<Expression<Boolean>> nextExpressions = specialiseOnAllPossibleValues(entry.getKey(), entry.getValue(), expr);
                        if(nextExpressions.contains(Condition.getFalse())){
                            next.clear();
                            next.add(Condition.getFalse());
                            break;
                        }
                        nextExpressions.removeIf((p) -> p.equals(Condition.getTrue()));
                        next.addAll(nextExpressions);
                    }
                    current = next;
                    if(current.size() == 1 && current.contains(Condition.getFalse())){
                        break;
                    }
                }

                for(Expression<Boolean> expr : current){
                    if(finalExpression == null){
                        finalExpression = expr.simplify();
                    } else{
                        finalExpression = new And(finalExpression, expr).simplify();
                    }
                }
                return finalExpression == null ? Condition.getTrue() : finalExpression;
            } else if(obss.getName().equals("exists")) {
                //t(exists(o)) = \/cv g_s && o
                Expression<Boolean> t = null;

                Set<Expression<Boolean>> current = new HashSet<>();
                current.add(new And(sendGuard, obss.getInput()));

                // TODO restrict to cvs that appear in obss
                for(Map.Entry<String, Type> nameTypePair : cvs.entrySet()){
                    Set<Expression<Boolean>> next = new HashSet<>();
                    for(Expression<Boolean> expr : current){
                        Set<Expression<Boolean>> nextExpressions = specialiseOnAllPossibleValues(nameTypePair.getKey(), nameTypePair.getValue(), expr);
                        if(nextExpressions.contains(Condition.getTrue())){
                            next.clear();
                            next.add(Condition.getTrue());
                            break;
                        }
                        nextExpressions.removeIf((p) -> p.equals(Condition.getFalse()));
                        next.addAll(nextExpressions);
                    }
                    current = next;
                    if(current.size() == 1 && current.contains(Condition.getTrue())){
                        break;
                    }
                }

                for(Expression<Boolean> expr : current){
                    if(t == null){
                        t = expr.simplify();
                    } else{
                        t = new Or(t, expr).simplify();
                    }
                }
                return t == null ? Condition.getFalse() : t;
            } else{
                throw new Exception("Predicate " + obss.getName() + " unknown.");
            }
        } else{
            return specialiseAtomicObservation(cvs, obs, sendGuard);
        }
    }

    private static Expression<Boolean> specialiseAtomicObservation(Map<String, Type> cvs,
            Expression<Boolean> atom, Expression<Boolean> sendGuard) throws Exception {
        AtomicReference<java.lang.Boolean> hasCommonVariable = new AtomicReference<>(false);
        atom.relabel(variable -> {
            String name = variable.getName().replaceFirst("^@", "");
            if (cvs.containsKey(name) || cvs.containsKey("@" + name)) hasCommonVariable.set(true);
            return variable;
        });
        // Finite comparison atoms use the same implicit universal scope as bare CVs.
        return hasCommonVariable.get()
            ? handleCVsInObservation(cvs, new Predicate("forall", atom), sendGuard) : atom;
    }

    public static Pair<List<LTOL>, Map<String, Observation>> ltolToLTLAndObservationVariables(List<LTOL> specs) throws Exception {
        Integer counter = 0;
        List<LTOL> pureLTLSpecs = new ArrayList<>();
        Map<String, Observation> observations = new HashMap<>();

        for(int i = 0; i < specs.size(); i++){
            LTOL ltol = specs.get(i);
            Triple<Integer, Map<String, Observation>, LTOL> integerMapLTOLTriple = ltol.abstractOutObservations(counter);
            counter = integerMapLTOLTriple.getLeft();
            observations.putAll(integerMapLTOLTriple.getMiddle());
            pureLTLSpecs.add(integerMapLTOLTriple.getRight());
        }

        return new Pair<>(pureLTLSpecs, observations);
    }

    public static String nuxmvTypeOfTypedVar(TypedVariable<?> typedVariable){
        Type type = typedVariable.getType();
        if(type.getClass().equals(Enum.class)){
            return "{" + String.join(",", ((Enum) type).getValues()) + "}";
        } else{
            return type.name();
        }
    }

    public static String indent(String text){
        String s = text.replaceAll("(?<=(^|\n))", "    ");
        return s;
    }

    public static String transform(System system) throws Exception {
        return transform(system, false);
    }

    //TODO use sendTagsAsVars
    public static String transform(System system, boolean sendTagsAsVars) throws Exception {
        return new NuXmvTranslation(system).translate();
    }

}
