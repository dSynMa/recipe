package recipe.lang.conditions;

import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Store;

public class Not extends Condition {

	private Condition arg;

	public Not(Condition arg) {
		super(Condition.PredicateType.NOT);
		if ((arg == null)) {
			throw new NullPointerException();
		}
		this.arg = arg;
	}

	
	@Override
	public boolean isSatisfiedBy(Store store) throws AttributeTypeException {
		return !arg.isSatisfiedBy(store);
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
			Not p = (Not) obj;
			return arg.equals(p.arg);
		}
		return false;
	}


	@Override
	public int hashCode() {
		return ~this.arg.hashCode();
	}

	@Override
	public String toString() {
		return "!(" + arg.toString() + ")";
	}

	public Condition getArgument() {
		return arg;
	}

	@Override
	public Condition close(Store store) {
		return new Not(arg.close(store));
	}

}
