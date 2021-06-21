package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Guard;
import recipe.lang.utils.TypingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        Expression expression = store.getDefinition(this.guardType.name()).getExpression();
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

    public static org.petitparser.parser.Parser parser(TypingContext context) throws Exception {
        Parser variable = context.variableParser();
        Parser value = context.valueParser();
        Parser guard = context.guardParser();

        Parser parser = (guard)
                .seq(CharacterParser.of('(').trim())
                //TODO parse also general expressions
                .seq(((variable.or(value)).separatedBy(CharacterParser.of(',').trim())).optional(new ArrayList<Expression>()))
                .map((List<Object> values) -> {
                    GuardReference guardReference = null;
                    String label = ((TypedVariable) values.get(0)).getName();
                    List<Expression> paramVals = (List<Expression>) values.get(2);
//                    List<String> varNames = new ArrayList<>();
//                    for(int i = 0; i < params.size(); i++){
//                        varNames.add(vars.get(i).getName());
//                    }

                    try{
                        Guard guardType = (Guard) context.get(label);
//                                Guard.getGuardDefinition(label);
                        TypedVariable[] params = guardType.getParameters();
                        for(int i = 0 ; i < paramVals.size(); i++){
                            if(!paramVals.get(i).getType().equals(params[i].getType())){
                                throw new MismatchingTypeException("Guard reference " + label + " is used with mismatching types of var: value is " + paramVals.get(i) + " but expected type " + params[i].getType() + ".");
                            }
                        }

                        return new GuardReference(guardType, paramVals.toArray(new Expression[paramVals.size()]));
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    return guardReference;
                });

        return parser;
    }
}