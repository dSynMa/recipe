package recipe.lang.conditions;

import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Attribute;
import recipe.lang.store.Store;

public class IsGreaterOrEqualThan extends Condition {

	private Number value;
	private String attribute;

	public IsGreaterOrEqualThan(String attribute, Number value) {
		super(Condition.PredicateType.ISGEQ);
		this.attribute = attribute;
		this.value = value;
	}
	public IsGreaterOrEqualThan(Attribute<?> attribute, Number value) {
		super(Condition.PredicateType.ISGEQ);
		this.attribute = attribute.getName();
		this.value = value;
	}

	
	@Override
	public boolean isSatisfiedBy(Store store) throws AttributeTypeException {
		Attribute<?> a = store.getAttribute(attribute);
		if (a == null) {
			return false;
		}
		Object v = store.getValue(a);
		if (v == null) {
			return false;
		}
		if (v instanceof Number) {
			return ((Number) v).doubleValue() >= value.doubleValue();
		}
		return false;
	}

	public String getAttribute() {
		return attribute;
	}

	public Number getValue() {
		return value;
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
			IsGreaterOrEqualThan p = (IsGreaterOrEqualThan) obj;
			if (!this.attribute.equals(p.attribute)) {
				return false;
			}
			return (this.value == p.value) || ((this.value != null) && (this.value.equals(p.value)));
		}
		return false;
	}

	
	@Override
	public int hashCode() {
		return attribute.hashCode() ^ value.hashCode();
	}

	@Override
	public String toString() {
		return attribute + ">=" + value.toString();
	}
	
	@Override
	public Condition close(Store localState) {
		Object o = localState.getValue(attribute);
		if (o == null) {
			return this;
		}
		try {
			if (this.isSatisfiedBy(localState)) {
				return Condition.TRUE;
			} else {
				return Condition.FALSE;
			}
		} catch (AttributeTypeException e) {
			return this;
		}
	}

}
