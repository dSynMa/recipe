package recipe.lang.expressions.predicate;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.utils.exceptions.*;

import java.util.function.Function;

public class And extends Condition {

	private Expression<Boolean> lhs;
	private Expression<Boolean> rhs;

	public And(Expression<Boolean> lhs, Expression<Boolean> rhs) {
		if ((lhs == null) || (rhs == null)) {
			throw new NullPointerException();
		}
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (super.equals(obj)) {
			And p = (And) obj;
			return lhs.equals(p.lhs) && rhs.equals(p.rhs);
		}
		return false;
	}

	@Override
	public String toString() {
		return "(" + lhs.toString() + ") & (" + rhs.toString() + ")";
	}

	public Expression<Boolean> getLhs() {
		return lhs;
	}

	public Expression<Boolean> getRhs() {
		return rhs;
	}

	@Override
	public TypedValue<Boolean> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, NotImplementedYetException {
		Expression lhsObject = lhs.valueIn(store);
		Expression rhsObject = rhs.valueIn(store);
		if (lhsObject.equals(Condition.TRUE) && rhsObject.equals(Condition.TRUE)) {
			return Condition.TRUE;
		} else if(lhsObject.equals(Condition.FALSE) || rhsObject.equals(Condition.FALSE)){
			return Condition.FALSE;
		} else{
			throw new AttributeTypeException();
		}
	}

	@Override
	public Expression<Boolean> simplify() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
		Expression<Boolean> lhsObject = lhs.simplify();
		Expression<Boolean> rhsObject = rhs.simplify();
		if (lhsObject.equals(Condition.TRUE) && rhsObject.equals(Condition.TRUE)) {
			return Condition.TRUE;
		} else if(lhsObject.equals(Condition.FALSE) || rhsObject.equals(Condition.FALSE)){
			return Condition.FALSE;
		} else if(lhsObject.equals(Condition.TRUE)){
			return rhsObject;
		} else if(rhsObject.equals(Condition.TRUE)){
			return lhsObject;
		} else {
			return new And(lhsObject, rhsObject);
		}
	}

	@Override
	public Condition relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
		return new And(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
	}
}
