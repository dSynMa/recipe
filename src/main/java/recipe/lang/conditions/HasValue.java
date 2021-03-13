package recipe.lang.conditions;

import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Attribute;
import recipe.lang.store.Store;

public class HasValue extends Condition {

	private Object value;

	private String attribute;

	public HasValue(String attribute, Object value) {
		super(Condition.PredicateType.ISEQUAL);
		this.attribute = attribute;
		this.value = value;
	}
	public HasValue(Attribute<?> attribute, Object value) {
		super(Condition.PredicateType.ISEQUAL);
		this.attribute = attribute.getName();
		this.value = value;
	}
	@Override
	public boolean isSatisfiedBy(Store store) throws AttributeTypeException {
		Attribute<?> a = store.getAttribute(attribute);
		if (a == null) {
			return false;
		}
		Object o = store.getValue(a);
		return (value == o) || ((value != null) && (value.equals(o)));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof HasValue) {
			HasValue hv = (HasValue) obj;
			return attribute.equals(hv.attribute) && value.equals(hv.value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return attribute.hashCode() ^ value.hashCode();
	}

	@Override
	public String toString() {
		return "{" + attribute + "==" + value + "}";
	}

	public String getAttribute() {
		return attribute;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public Condition close(Store store) {
		Object o = store.getValue(attribute);
		if (o == null) {
			return this;
		}
		if (o.equals(value)) {
			return Condition.TRUE;
		} else {
			return Condition.FALSE;
		}
	}
	

}

