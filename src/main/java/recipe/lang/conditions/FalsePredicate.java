package recipe.lang.conditions;

import recipe.lang.store.Store;

public class FalsePredicate extends Condition {

	public FalsePredicate() {
		super(Condition.PredicateType.FALSE);
	}

	@Override
	public boolean isSatisfiedBy(Store store) {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		return obj instanceof FalsePredicate;
	}

	@Override
	public int hashCode() {
		return "false".hashCode();
	}

	@Override
	public String toString() {
		return "false";
	}

	@Override
	public Condition close(Store store) {
		return this;
	}
	
	

}