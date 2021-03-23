package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.arithmetic.NumberValue;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.Store;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class IsGreaterOrEqualThan extends Condition {

	private ArithmeticExpression lhs;
	private ArithmeticExpression rhs;

	public IsGreaterOrEqualThan(ArithmeticExpression lhs, ArithmeticExpression rhs) {
		super(Condition.PredicateType.ISGEQ);
		this.lhs = lhs;
		this.rhs = rhs;
	}

//	public IsGreaterOrEqualThan(Attribute<?> attribute, Number value) {
//		super(Condition.PredicateType.ISGEQ);
//		this.lhs = new Variable(attribute.getName());
//		this.rhs = new Value(value);
//	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (super.equals(obj)) {
			IsGreaterOrEqualThan p = (IsGreaterOrEqualThan) obj;
			if (!this.lhs.equals(p.lhs)) {
				return false;
			}
			return (this.rhs == p.rhs) || ((this.rhs != null) && (this.rhs.equals(p.rhs)));
		}
		return false;
	}

	
	@Override
	public int hashCode() {
		return lhs.hashCode() ^ rhs.hashCode();
	}

	@Override
	public String toString() {
		return lhs + ">=" + rhs.toString();
	}

	@Override
	public BooleanValue valueIn(Store store) throws AttributeTypeException, AttributeNotInStoreException {
		NumberValue lhsValue = lhs.valueIn(store);
		NumberValue rhsValue = rhs.valueIn(store);

		Number lhsNo = lhsValue.value;
		Number rhsNo = rhsValue.value;

		if(0 >= new BigDecimal(lhsNo.toString()).compareTo(new BigDecimal(rhsNo.toString()))) {
			return Condition.TRUE;
		} else {
			return Condition.FALSE;
		}
	}

	@Override
	public Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
		ArithmeticExpression lhsObject = lhs.close(store, CV);
		ArithmeticExpression rhsObject = rhs.close(store, CV);
		if (lhsObject.equals(rhsObject)) {
			return Condition.TRUE;
		} else if(!lhsObject.getClass().equals(NumberValue.class) ||
				!rhsObject.getClass().equals(NumberValue.class)){
			return new IsGreaterOrEqualThan(lhsObject, rhsObject);
		} else{
			return Condition.FALSE;
		}
	}

	public static org.petitparser.parser.Parser parser(Parser arithmeticExpression) {
		org.petitparser.parser.Parser parser =
				(arithmeticExpression)
						.seq(StringParser.of(">=").trim())
						.seq(arithmeticExpression)
						.map((List<Object> values) -> {
							return new IsGreaterOrEqualThan((ArithmeticExpression) values.get(0), (ArithmeticExpression) values.get(2));
						});

		return parser;
	}
}
