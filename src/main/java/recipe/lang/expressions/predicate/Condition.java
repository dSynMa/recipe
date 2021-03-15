package recipe.lang.expressions.predicate;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.store.Store;

import java.util.Set;

//TODO add exception wrong type and non-existing variables
public abstract class Condition extends Expression {

	public enum PredicateType {
		FALSE, TRUE, ISEQUAL, ISGTR, ISGEQ, ISLEQ, ISLES, AND, OR, NOT, VAR
	}

	public static final BooleanValue TRUE = new BooleanValue(true);
	public static final BooleanValue FALSE = new BooleanValue(false);

	private PredicateType type;

	public boolean isSatisfiedBy(Store store) throws AttributeTypeException, AttributeNotInStoreException{
		BooleanValue value = valueIn(store);
		if(value.equals(TRUE)){
			return true;
		} else{
			return false;
		}
	}

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

	public abstract BooleanValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
	public abstract Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;
}
