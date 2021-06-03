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

        Parser parser = (CharacterParser.word().plus().flatten())
                .seq(CharacterParser.of('(').trim())
                .seq(variable.separatedBy(CharacterParser.of(',').trim()))
                .map((List<Object> values) -> {
                    GuardReference guardReference = null;
                    String label = (String) values.get(0);
                    List<TypedVariable> vars = (List<TypedVariable>) values.get(2);
                    List<String> varNames = new ArrayList<>();
                    for(int i = 0; i < vars.size(); i++){
                        varNames.add(vars.get(i).getName());
                    }

                    try{
                        Guard guardType =
                                Guard.getGuardDefinition(label);
                        TypedVariable[] params = guardType.getParameters();
                        for(int i = 0 ; i < vars.size(); i++){
                            if(!vars.get(i).getType().equals(params[i])){
                                throw new MismatchingTypeException(label + "(" + String.join(", ", varNames) + ") has mismatching types of var " + vars.get(i).getName() + " with expected " + params[i].toString() + ".");
                            }
                        }

                        return new GuardReference(guardType, vars.toArray(new TypedVariable[0]));
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    return guardReference;
                });

        return parser;
    }
}