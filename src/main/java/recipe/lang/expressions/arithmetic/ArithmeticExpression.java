package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.Parser;
import org.petitparser.tools.ExpressionBuilder;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Boolean;
import recipe.lang.types.Number;
import recipe.lang.store.Store;
import recipe.lang.types.Real;
import recipe.lang.types.Type;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static org.petitparser.parser.primitive.CharacterParser.of;

public abstract class ArithmeticExpression implements Expression<Number> {
    public abstract TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, NotImplementedYetException;

    public abstract Expression<Number> simplify() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException;

    public static Parser typeParser(TypingContext context) throws Exception {
        return ArithmeticExpression.parser(context);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context) throws Exception {
        ExpressionBuilder builder = new ExpressionBuilder();

        org.petitparser.parser.Parser value = Real.getType().valueParser().or(recipe.lang.types.Integer.getType().valueParser());
        org.petitparser.parser.Parser variable = context.getSubContext(Real.getType()).variableParser()
                .or(context.getSubContext(recipe.lang.types.Integer.getType()).variableParser());

        builder.group()
                .primitive(variable.or(value).trim())
                .wrapper(of('(').trim(), of(')').trim(),
                        (List<Expression<Number>> values) -> values.get(1));

        // multiplication and addition are left-associative
        builder.group()
                .left(of('*').trim(), (List<ArithmeticExpression> values) -> new Multiplication(values.get(0), values.get(2)))
                .left(of('/').trim(), (List<ArithmeticExpression> values) -> new Division(values.get(0), values.get(2)));
        builder.group()
                .left(of('+').trim(), (List<ArithmeticExpression> values) -> new Addition(values.get(0), values.get(2)))
                .left(of('-').trim(), (List<ArithmeticExpression> values) -> new Subtraction(values.get(0), values.get(2)));

        return builder.build();
    }

    @Override
    public abstract Expression<Number> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException;

    @Override
    public int hashCode(){
        return Objects.hash(this.toString());
    }

    @Override
    public java.lang.Boolean isValidAssignmentFor(TypedVariable var){
        return Number.class.isAssignableFrom(var.getType().getClass());
    }

    @Override
    public Type getType(){
        return Real.getType();
    }

    // Helper function to check if n is integer or real
    protected static boolean isInteger(BigDecimal n) {
        return n.signum() == 0 || n.scale() <= 0 || n.stripTrailingZeros().scale() <= 0;
    }

    public Set<Expression<Boolean>> subformulas(){
        Set<Expression<recipe.lang.types.Boolean>> subformulas = new HashSet<>();
        return subformulas;
    }

    // TODO this is not implemented fully for the arithmetic operations, only at the top level
    public Expression<Number> replace(java.util.function.Predicate<Expression<Number>> cond,
                                 Function<Expression<Number>, Expression<Number>> act) {
        if (cond.test(this)) {
            return act.apply(this);
        } else {
            return this;
        }
    }

    public Expression<recipe.lang.types.Number> removePreds(){
        return this;
    }
}
