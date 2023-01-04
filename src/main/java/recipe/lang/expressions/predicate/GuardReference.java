package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;

import recipe.lang.definitions.GuardDefinition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.CompositeStore;
import recipe.lang.store.ConcreteStore;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Guard;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.*;

import java.util.*;
import java.util.function.Function;

public class GuardReference extends Condition {
    private Guard guardType;
    private Expression[] parametersValues;
    public static boolean resolve = false;

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

        GuardDefinition guardDef = Guard.getDefinition(guardType.name());
        Map<TypedVariable, TypedValue> paramsMap = new HashMap<TypedVariable, TypedValue>();

        // Evaluate parameters and bind values to their identifiers
        TypedVariable[] params = guardType.getParameters();
        try {
            for (int i = 0; i < parametersValues.length; i++) {
                TypedValue val = parametersValues[i].valueIn(store);
                // System.err.printf("param %s has value %s which yields %s\n", params[i], parametersValues[i], val);
                paramsMap.put(params[i], val);
            }
            // Evaluate guard itself
            CompositeStore cs = new CompositeStore();
            cs.push(store);
            cs.push(new ConcreteStore(paramsMap));
            // System.err.printf("guard: %s\n", guardDef.getTemplate());
            return guardDef.getTemplate().valueIn(cs);
        } catch (NotImplementedYetException e) {
            // TODO
            System.err.println(e);
            System.err.println(e.getStackTrace());
        }
        return null;
    }

    @Override
    public Expression<Boolean> simplify() throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, TypeCreationException, RelabellingTypeException {
        return this.unpack().simplify();
    }

    @Override
    public Expression<Boolean> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        Expression<Boolean> unpacked = this.unpack();

        return unpacked.relabel(relabelling);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context, SettableParser conditionParser) throws Exception {
        Parser expression = conditionParser.or(ArithmeticExpression.parser(context))
                .or(context.variableParser())
                .or(Enum.generalValueParser());

        Parser guard = context.guardNameParser();

        Parser parser = (guard.map((Object v) -> {
            return v;
        }))
                .seq(CharacterParser.of('(').trim())
                .seq(((expression).separatedBy(CharacterParser.of(',').trim()).trim()).optional(new ArrayList<>()).map((Object v) -> {
                    return v;
                }))
                .seq(CharacterParser.of(')').trim())
                .map((List<Object> values) -> {
                    GuardReference guardReference = null;
                    String label = (String) values.get(0);
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

    public Expression<Boolean> unpack() throws RelabellingTypeException, MismatchingTypeException {
        GuardDefinition guardDefinition = Guard.getDefinition(this.guardType.name());
        Expression<Boolean> template = guardDefinition.getTemplate();
        List params = List.of(guardDefinition.getType().getParameters());
        return template.relabel((x) -> {
            if (params.contains(x)) {
                return this.parametersValues[params.indexOf(x)];
            } else {
                return x;
            }
        });
    }

    public String toString(){
        if(resolve) {
            try {
                return unpack().toString();
            } catch (RelabellingTypeException e) {
                e.printStackTrace();
            } catch (MismatchingTypeException e) {
                e.printStackTrace();
            }

            return toString();
        } else{
            return guardType.name() + "(" + String.join(",", Arrays.stream(parametersValues).map((x) -> x.toString()).toArray(String[]::new)) + ")";
        }
    }
}