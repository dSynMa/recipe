package recipe.lang.expressions.arithmetic;

import java.math.BigDecimal;
import java.math.BigInteger;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Type;

/** Conservative declared-domain checks for numeric common-variable mappings. */
public final class NumericDomain {
    private NumericDomain() {}

    public static boolean isSubsetOf(Expression expression, Type target) {
        Domain source = domain(expression);
        if (!source.integral) return false;
        if (!(target instanceof BoundedInteger)) return true;
        BoundedInteger bounds = (BoundedInteger) target;
        return source.min != null && source.max != null
            && source.min.compareTo(BigInteger.valueOf(bounds.getMin())) >= 0
            && source.max.compareTo(BigInteger.valueOf(bounds.getMax())) <= 0;
    }

    private static Domain domain(Expression expression) {
        if (expression instanceof TypedValue) {
            try {
                BigInteger value = new BigDecimal(((TypedValue) expression).getValue().toString()).toBigIntegerExact();
                return new Domain(value, value, true);
            } catch (ArithmeticException e) {
                return new Domain(null, null, false);
            }
        }
        if (expression instanceof Addition) {
            Addition sum = (Addition) expression;
            return combine(domain(sum.lhs), domain(sum.rhs), '+');
        }
        if (expression instanceof Subtraction) {
            Subtraction difference = (Subtraction) expression;
            return combine(domain(difference.lhs), domain(difference.rhs), '-');
        }
        if (expression instanceof Multiplication) {
            Multiplication product = (Multiplication) expression;
            return combine(domain(product.lhs), domain(product.rhs), '*');
        }
        if (expression instanceof Division) {
            Division quotient = (Division) expression;
            Domain divisor = domain(quotient.rhs);
            // Division is real-valued; only division by +/-1 preserves every integer.
            if (divisor.min != null && divisor.min.equals(divisor.max)
                    && divisor.min.abs().equals(BigInteger.ONE)) {
                return combine(domain(quotient.lhs), divisor, '*');
            }
            return new Domain(null, null, false);
        }
        if (expression.getType() instanceof BoundedInteger) {
            BoundedInteger bounds = (BoundedInteger) expression.getType();
            return new Domain(BigInteger.valueOf(bounds.getMin()), BigInteger.valueOf(bounds.getMax()), true);
        }
        return new Domain(null, null, expression.getType() instanceof recipe.lang.types.Integer);
    }

    private static Domain combine(Domain left, Domain right, char operator) {
        boolean integral = left.integral && right.integral;
        if (left.min == null || left.max == null || right.min == null || right.max == null)
            return new Domain(null, null, integral);
        if (operator == '+') return new Domain(left.min.add(right.min), left.max.add(right.max), integral);
        if (operator == '-') return new Domain(left.min.subtract(right.max), left.max.subtract(right.min), integral);
        BigInteger[] products = {left.min.multiply(right.min), left.min.multiply(right.max),
            left.max.multiply(right.min), left.max.multiply(right.max)};
        BigInteger min = products[0], max = products[0];
        for (BigInteger product : products) {
            min = min.min(product);
            max = max.max(product);
        }
        return new Domain(min, max, integral);
    }

    private static final class Domain {
        final BigInteger min, max;
        final boolean integral;
        Domain(BigInteger min, BigInteger max, boolean integral) {
            this.min = min;
            this.max = max;
            this.integral = integral;
        }
    }
}
