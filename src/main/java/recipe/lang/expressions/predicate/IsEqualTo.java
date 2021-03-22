package recipe.lang.expressions.predicate;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.store.Store;

import java.util.Set;

public class IsEqualTo extends Condition {

	private Expression lhs;
	private Expression rhs;

	public IsEqualTo(Expression lhs, Expression rhs) {
		super(Condition.PredicateType.ISEQUAL);
		this.lhs = lhs;
		this.rhs = rhs;
	}
//	public IsEqualTo(Attribute<?> attribute, Object value) {
//		super(Condition.PredicateType.ISEQUAL);
//		this.lhs = new BooleanVariable(attribute.getName());
//		this.rhs = new BooleanValue(value);
//	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof IsEqualTo) {
			IsEqualTo hv = (IsEqualTo) obj;
			return lhs.equals(hv.lhs) && rhs.equals(hv.rhs);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return lhs.hashCode() ^ rhs.hashCode();
	}

	@Override
	public String toString() {
		return "{" + lhs + "==" + rhs + "}";
	}

	@Override
	public BooleanValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
		Expression lhsValue = lhs.valueIn(store);
		Expression rhsValue = rhs.valueIn(store);

		if(lhsValue.equals(rhsValue)){
			return Condition.TRUE;
		} else{
			return Condition.FALSE;
		}
	}

	@Override
	public Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
		Expression lhsObject = lhs.close(store, CV);
		Expression rhsObject = rhs.close(store, CV);
		if (lhsObject.equals(rhsObject)) {
			return Condition.TRUE;
		} else if(!lhsObject.getClass().equals(BooleanValue.class) &&
				!rhsObject.getClass().equals(BooleanValue.class)){
			return new IsEqualTo(lhsObject, rhsObject);
		} else{
			return Condition.FALSE;
		}
	}
	

}

