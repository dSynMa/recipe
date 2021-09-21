package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.definitions.GuardDefinition;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Guard;
import recipe.lang.utils.LazyParser;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.*;
import java.util.function.Function;

public class GuardReference extends Condition {
    private Guard guardType;
    private Expression[] parametersValues;

    public GuardReference(Guard guardType, Expression[] parametersValues) throws MismatchingTypeException {
        this.guardType = guardType;
        TypedVariable[] params = guardType.getParameters();
        this.parametersValues = new Expression[parametersValues.length];

        for (int i = 0; i < params.length; i++) {
            TypedVariable typedVariable = params[i];
            Expression value = parametersValues[i];

            if (value.isValidAssignmentFor(typedVariable)) {
                this.parametersValues[i] = value;
            } else {
                throw new MismatchingTypeException(value.toString() + " is not a valid value for " + typedVariable.toString() + " in guard definition " + guardType.name() + ".");
            }
        }
    }

    @Override
    public TypedValue<Boolean> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException {
        return null;
    }

    @Override
    public Expression<Boolean> close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, TypeCreationException, RelabellingTypeException {
        Expression expression = (Expression) store.getDefinition(this.guardType.name()).getTemplate();
        for(TypedVariable v : guardType.getParameters()){
            expression = expression.relabel(vv -> v);
        }

        return expression.close(store, CV);
    }

    @Override
    public GuardReference relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        Expression[] newParameterValues = new Expression[parametersValues.length];
        for(int i = 0; i < parametersValues.length; i++){
            newParameterValues[i] = parametersValues[i].relabel(relabelling);
        }
        return new GuardReference(guardType, newParameterValues);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context, Parser basicConditionParser) throws Exception {
        Parser expression = basicConditionParser.or(ArithmeticExpression.typeParser(context));

        Parser guard = context.guardParser();

        Parser parser = (guard)
                .seq(CharacterParser.of('(').trim())
                .seq(((expression).separatedBy(CharacterParser.of(',').trim()).trim()).optional())
                .seq(CharacterParser.of(')').trim())
                .map((List<Object> values) -> {
                    GuardReference guardReference = null;
                    String label = ((TypedVariable) values.get(0)).getName();
                    List<Object> paramValsUntyped = (List<Object>) values.get(2);
                    paramValsUntyped.removeIf(x -> Objects.equals(x, Character.valueOf(',')));
                    List<Expression> paramValsTyped = new ArrayList<>();

                    try{
                        Guard guardType = (Guard) context.get(label);
//                                Guard.getGuardDefinition(label);
                        TypedVariable[] params = guardType.getParameters();
                        for(int i = 0 ; i < paramValsUntyped.size(); i++){
                            if(!paramValsUntyped.get(i).equals(',')){
                                Expression paramVal = (Expression) paramValsUntyped.get(i);
                                paramValsTyped.add(paramVal);
                                if(!(paramVal.isValidAssignmentFor(params[i]))) {
                                    throw new MismatchingTypeException("Guard reference " + label + " is used with mismatching types of var: value is " + paramVal.getType() + " but expected type " + params[i].getType() + ".");
                                }
                            }
                        }

                        return new GuardReference(guardType, paramValsTyped.toArray(new Expression[paramValsTyped.size()]));
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    return guardReference;
                });

        return parser;
    }

    public String toString(){
        GuardDefinition guardDefinition = Guard.getDefinition(this.guardType.name());
        Expression<Boolean> template = guardDefinition.getTemplate();
        List params = List.of(guardDefinition.getType().getParameters());

        try {
            return template.relabel((x) -> {
                if(params.contains(x)){
                    return this.parametersValues[params.indexOf(x)];
                } else {
                    return x;
                }
            }).toString();
        } catch (RelabellingTypeException e) {
            e.printStackTrace();
        } catch (MismatchingTypeException e) {
            e.printStackTrace();
        }

        return toOriginalString();
    }

    public String toOriginalString(){
        return guardType.name() + "(" + String.join(",", Arrays.stream(parametersValues).map((x) -> x.toString()).toArray(String[]::new)) + ")";
    }
}