package recipe.lang.conditions;

import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Attribute;
import recipe.lang.store.Store;

public class IsLessOrEqualThan extends Condition {

	private Number value;
	private String attribute;

	public IsLessOrEqualThan(String attribute, Number value) {
		super(Condition.PredicateType.ISLEQ);
		this.attribute = attribute;
		this.value = value;
	}
	public IsLessOrEqualThan(Attribute<?> attribute, Number value) {
		super(Condition.PredicateType.ISLEQ);
		this.attribute = attribute.getName();
		this.value = value;
	}

	
	@Override
	public boolean isSatisfiedBy(Store store) throws AttributeTypeException {
		Attribute<?> a = store.getAttribute(attribute);
		Object v = (a != null ? store.getValue(a) : null);
		if ((v != null) && (v instanceof Number)) {
			return ((Number) v).doubleValue() <= value.doubleValue();
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
			IsLessOrEqualThan p = (IsLessOrEqualThan) obj;
			if (!this.attribute.equals(p.attribute)) {
				return false;
			}
			return (this.value == p.value) || ((this.value != null) && (this.value.equals(p.value)));
		}
		return false;
	}

	
	@Override
	public int hashCode() {
		return attribute.hashCode() ^ (value == null ? 0 : value.hashCode());
	}

	@Override
	public String toString() {
		return attribute + ">" + value;
	}
	
	@Override
	public Condition close(Store store) {
		Object o = store.getValue(attribute);
		if (o == null) {
			return this;
		}
		try {
			if (this.isSatisfiedBy(store)) {
				return Condition.TRUE;
			} else {
				return Condition.FALSE;
			}
		} catch (AttributeTypeException e) {
			return this;
		}
	}

}