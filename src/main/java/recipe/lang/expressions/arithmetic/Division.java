package recipe.lang.expressions.arithmetic;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.IsNotEqualTo;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Number;
import recipe.lang.types.Boolean;
import recipe.lang.types.Real;
import recipe.lang.utils.exceptions.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.Function;

public class Division extends ArithmeticExpression{
    Expression<Number> lhs;
    Expression<Number> rhs;

    public Division(Expression<Number> lhs, Expression<Number> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public Expression<Boolean> definednessCondition()
            throws RelabellingTypeException, MismatchingTypeException {
        Expression<Boolean> operands = new And(lhs.definednessCondition(), rhs.definednessCondition());
        return new And(operands,
                new IsNotEqualTo(rhs,
                    new TypedValue<>(recipe.lang.types.Integer.getType(), "0")));
    }

    @Override
    public TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, NotImplementedYetException {
        TypedValue<Number> lhsValue = lhs.valueIn(store);
        TypedValue<Number> rhsValue = rhs.valueIn(store);

        BigDecimal lhsNo = new BigDecimal(lhsValue.getValue().toString());
        BigDecimal rhsNo = new BigDecimal(rhsValue.getValue().toString());
        BigDecimal result;
        try {
            result = lhsNo.divide(rhsNo);
        } catch (ArithmeticException nonTerminatingDecimal) {
            if (rhsNo.signum() == 0) throw nonTerminatingDecimal;
            // Only nonterminating quotients need a finite precision approximation.
            result = lhsNo.divide(rhsNo, MathContext.DECIMAL128);
        }
        return new TypedValue<Number>(Real.getType(), result.toString());
    }

    @Override
    public Expression<Number> simplify() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
        Expression<Number> lhsValue = lhs.simplify();
        Expression<Number> rhsValue = rhs.simplify();

        if (lhsValue.getClass().equals(TypedValue.class)
                && rhsValue.getClass().equals(TypedValue.class)){
            BigDecimal lhsNo = new BigDecimal(((TypedValue) lhsValue).getValue().toString());
            BigDecimal rhsNo = new BigDecimal(((TypedValue) rhsValue).getValue().toString());
            try {
                return numericResult(lhsNo.divide(rhsNo));
            } catch (ArithmeticException nonTerminatingDecimal) {
                if (rhsNo.signum() == 0) return new Division(lhsValue, rhsValue);
                // Keep the exact quotient for symbolic analysis. A real numerator
                // prevents nuXmv from interpreting integer literals as integer division.
                return new Division(new TypedValue<Number>(Real.getType(), lhsNo.toString()), rhsValue);
            }
        }
        else {
            return new Division(lhsValue, rhsValue);
        }
    }

    @Override
    public ArithmeticExpression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Division(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
    }

    @Override
    public String toNuXmv() {
        // Force real division, including when both operands have integer domains.
        return "((1.0 * " + lhs.toNuXmv() + ") / " + rhs.toNuXmv() + ")";
    }

    @Override
    public String toString(){
        // Finite-domain integer operands otherwise use truncated division in nuXmv's BDD backend.
        // Our evaluator and constant folding use mathematical (real) division.
        return "((1.0 * " + lhs.toString() + ") / " + rhs.toString() + ")";
    }
}
