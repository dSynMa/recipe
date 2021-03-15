package recipe.lang.expressions.predicate;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.Store;

import java.math.BigDecimal;
import java.util.Set;

public class IsGreaterThan extends Condition {

	private ArithmeticExpression lhs;
	private ArithmeticExpression rhs;

	public IsGreaterThan(ArithmeticExpression lhs, ArithmeticExpression rhs) {
		super(Condition.PredicateType.ISGTR);
		this.lhs = lhs;
		this.rhs = rhs;
	}
//	public IsGreaterThan(Attribute<?> attribute, Number value) {
//		super(Condition.PredicateType.ISGTR);
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
			IsGreaterThan p = (IsGreaterThan) obj;
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
		Expression lhsValue = lhs.valueIn(store);
		Expression rhsValue = rhs.valueIn(store);

		try {
			Number lhsNo = (Number) ((Value) lhsValue).value;
			Number rhsNo = (Number) ((Value) rhsValue).value;

			if(0 > new BigDecimal(lhsNo.toString()).compareTo(new BigDecimal(rhsNo.toString()))) {
				return Condition.TRUE;
			} else {
				return Condition.FALSE;
			}
		} catch (Exception e){
			throw new AttributeTypeException();
		}
	}

	@Override
	public Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException {
		ArithmeticExpression lhsObject = lhs.close(store, CV);
		ArithmeticExpression rhsObject = rhs.close(store, CV);
		if (lhsObject.equals(rhsObject)) {
			return Condition.TRUE;
		} else if(!lhsObject.getClass().equals(Value.class) ||
				!rhsObject.getClass().equals(Value.class)){
			return new IsGreaterThan(lhsObject, rhsObject);
		} else{
			return Condition.FALSE;
		}
	}

}
