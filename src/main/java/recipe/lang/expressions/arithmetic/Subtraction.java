package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Number;
import recipe.lang.types.Real;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class Subtraction extends ArithmeticExpression{
    Expression<Number> lhs;
    Expression<Number> rhs;

    public Subtraction(Expression<Number> lhs, Expression<Number> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException {
        TypedValue<Number> lhsValue = lhs.valueIn(store);
        TypedValue<Number> rhsValue = rhs.valueIn(store);

        if(lhsValue.getValue().getClass().equals(Number.class) &&
                rhsValue.getValue().getClass().equals(Number.class)){
            BigDecimal lhsNo = new BigDecimal(lhsValue.getValue().toString());
            BigDecimal rhsNo = new BigDecimal(rhsValue.getValue().toString());
            return new TypedValue<Number>((Number) Real.getType(), lhsNo.subtract(rhsNo).toString());
        }
        throw new AttributeTypeException();
    }

    @Override
    public Expression<Number> close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
        return new Subtraction(lhs.close(store, CV), rhs.close(store, CV));
    }

    @Override
    public Expression<Number> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Subtraction(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
    }

    @Override
    public String toString(){
        return "(" + lhs.toString() + " - " + rhs.toString() + ")";
    }
}
