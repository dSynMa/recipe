package recipe.lang.conditions;

import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Store;

//TODO add exception wrong type and non-existing variables
public abstract class Condition {

	public enum PredicateType {
		FALSE, TRUE, ISEQUAL, ISGTR, ISGEQ, ISLEQ, ISLES, AND, OR, NOT
	}

	public static final Condition TRUE = new TruePredicate();
	public static final Condition FALSE = new FalsePredicate();

	private PredicateType type;

	public abstract boolean isSatisfiedBy(Store store) throws AttributeTypeException;

	public Condition(PredicateType type) {
		this.type = type;
	}


	public PredicateType getType() {
		return this.type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof Condition) {
			return this.type == ((Condition) obj).type;
		}
		return false;
	}

	public abstract Condition close(Store store);

}
