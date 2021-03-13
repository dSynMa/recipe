package recipe.lang.conditions;

import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Attribute;
import recipe.lang.store.Store;

public class IsGreaterThan extends Condition {

	private Number value;
	private String attribute;

	public IsGreaterThan(String attribute, Number value) {
		super(Condition.PredicateType.ISGTR);
		this.attribute = attribute;
		if (value != null) {
			this.value = value;
		} else {
			this.value = 0;
		}
	}
	public IsGreaterThan(Attribute<?> attribute, Number value) {
		super(Condition.PredicateType.ISGTR);
		this.attribute = attribute.getName();
		if (value != null) {
			this.value = value;
		} else {
			this.value = 0;
		}
	}
	
	@Override
	public boolean isSatisfiedBy(Store store) throws AttributeTypeException {
		Attribute<?> a = store.getAttribute(attribute);
		Object v = store.getValue(a);
		if (v instanceof Number) {
			return ((Number) v).doubleValue() > value.doubleValue();
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
			IsGreaterThan p = (IsGreaterThan) obj;
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