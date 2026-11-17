package recipe.lang.expressions.arithmetic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Number;
import recipe.lang.utils.exceptions.*;

/** Internal arithmetic operation used to check integer update feasibility. */
public class Floor extends ArithmeticExpression {
    private final Expression<Number> operand;

    public Floor(Expression<Number> operand) {
        this.operand = operand;
    }

    @Override
    public TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException,
            MismatchingTypeException, NotImplementedYetException {
        BigDecimal value = new BigDecimal(operand.valueIn(store).getValue().toString());
        return numericResult(value.setScale(0, RoundingMode.FLOOR));
    }

    @Override
    public Expression<Number> simplify() throws AttributeNotInStoreException, AttributeTypeException,
            TypeCreationException, MismatchingTypeException, RelabellingTypeException {
        Expression<Number> value = operand.simplify();
        if (isIntegral(value)) return value;
        if (value instanceof TypedValue) {
            return numericResult(new BigDecimal(((TypedValue) value).getValue().toString())
                    .setScale(0, RoundingMode.FLOOR));
        }
        return new Floor(value);
    }

    // BDD models need no real-arithmetic floor node for integer-only expressions.
    private static boolean isIntegral(Expression<?> value) {
        if (value.getType() instanceof recipe.lang.types.Integer || value instanceof Floor) return true;
        if (value instanceof Addition) {
            Addition sum = (Addition) value;
            return isIntegral(sum.lhs) && isIntegral(sum.rhs);
        }
        if (value instanceof Subtraction) {
            Subtraction difference = (Subtraction) value;
            return isIntegral(difference.lhs) && isIntegral(difference.rhs);
        }
        if (value instanceof Multiplication) {
            Multiplication product = (Multiplication) value;
            return isIntegral(product.lhs) && isIntegral(product.rhs);
        }
        return false;
    }

    @Override
    public ArithmeticExpression relabel(Function<TypedVariable, Expression> relabelling)
            throws RelabellingTypeException, MismatchingTypeException {
        return new Floor(operand.relabel(relabelling));
    }

    @Override
    public String toNuXmv() {
        String value = operand.toNuXmv();
        return isIntegral(operand) ? value : "floor(" + value + ")";
    }

    @Override
    public String toString() {
        return isIntegral(operand) ? operand.toString() : "floor(" + operand + ")";
    }
}
