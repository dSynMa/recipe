package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.arithmetic.NumberValue;
import recipe.lang.store.Store;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class Subtraction extends ArithmeticExpression{
    ArithmeticExpression lhs;
    ArithmeticExpression rhs;

    public Subtraction(ArithmeticExpression lhs, ArithmeticExpression rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        NumberValue lhsValue = lhs.valueIn(store);
        NumberValue rhsValue = rhs.valueIn(store);

        if(lhsValue.value.getClass().equals(Number.class) &&
                rhsValue.value.getClass().equals(Number.class)){
            BigDecimal lhsNo = new BigDecimal(lhsValue.value.toString());
            BigDecimal rhsNo = new BigDecimal(rhsValue.value.toString());
            return new NumberValue(lhsNo.subtract(rhsNo));
        }
        throw new AttributeTypeException();
    }

    @Override
    public ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
        return new Subtraction(lhs.close(store, CV), rhs.close(store, CV));
    }

    @Override
    public String toString(){
        return "(" + lhs.toString() + "-" + rhs.toString() + ")";
    }

    public static org.petitparser.parser.Parser parser(Parser bracketedArithmeticExpression) {
        org.petitparser.parser.Parser value = NumberValue.parser();
        org.petitparser.parser.Parser variable = NumberVariable.parser();
        org.petitparser.parser.Parser myVariable = MyNumberVariable.parser();

        org.petitparser.parser.Parser parser =
                (value.or(variable).or(myVariable).or(bracketedArithmeticExpression))
                        .seq(CharacterParser.of('-').trim())
                        .seq((value.or(variable).or(myVariable).or(bracketedArithmeticExpression)))
                        .map((List<Object> values) -> {
                            return new Subtraction((ArithmeticExpression) values.get(0), (ArithmeticExpression) values.get(2));
                        });

        return parser;
    }
}
