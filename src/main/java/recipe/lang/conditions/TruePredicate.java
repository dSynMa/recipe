package recipe.lang.conditions;

import recipe.lang.store.Store;

public class TruePredicate extends Condition {

	public TruePredicate() {
		super(Condition.PredicateType.TRUE);
	}

	@Override
	public boolean isSatisfiedBy(Store store) {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		return obj instanceof TruePredicate;
	}

	@Override
	public int hashCode() {
		return "true".hashCode();
	}

	@Override
	public String toString() {
		return "true";
	}

	@Override
	public Condition close(Store store) {
		return this;
	}
	
	

}
