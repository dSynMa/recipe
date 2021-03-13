package recipe.lang.conditions;

import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Store;

public class Or extends Condition {

	private Condition left;
	private Condition right;

	public Or(Condition left, Condition right) {
		super(Condition.PredicateType.OR);
		if ((left == null) || (right == null)) {
			throw new NullPointerException();
		}
		this.left = left;
		this.right = right;
	}


	@Override
	public boolean isSatisfiedBy(Store store) throws AttributeTypeException {
		return left.isSatisfiedBy(store) || right.isSatisfiedBy(store);
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
			Or p = (Or) obj;
			return left.equals(p.left) && right.equals(p.right);
		}
		return false;
	}


	@Override
	public int hashCode() {
		return this.left.hashCode() ^ this.right.hashCode();
	}

	
	@Override
	public String toString() {
		return "(" + left.toString() + ") || (" + right.toString() + ")";
	}

	public Condition getLeft() {
		return left;
	}

	public Condition getRight() {
		return right;
	}

	@Override
	public Condition close(Store store) {
		return new Or(left.close(store), right.close(store)	);
	}
}
