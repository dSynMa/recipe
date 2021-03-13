package recipe.lang.store;

import java.util.HashMap;
import java.util.Map;

import recipe.lang.conditions.Condition;
import recipe.lang.exception.AttributeTypeException;

public class Store {
    private HashMap<String, Object> data;

	private HashMap<String, Attribute<?>> attributes;

	
	public Map<String, Attribute<?>> getAttributes() {
		return attributes;
	}

	public Store(Map<String, Object> data, Map<String, Attribute<?>> attributes) {
		this.data = new HashMap<>(data);
		this.attributes = new HashMap<>(attributes);
	}

	public Store() {
		this.data = new HashMap<>();
		this.attributes = new HashMap<>();
	}

	protected boolean isConsistent(Attribute<?> attribute) {
		Attribute<?> old = attributes.get(attribute.getName());
		if (old == null) {
			attributes.put(attribute.getName(), attribute);
			return true;
		}
		return old.equals(attribute);
	}

	public  <T> T getValue(Attribute<T> attribute) throws AttributeTypeException {
		Object o = data.get(attribute.getName());
		if (o == null) {
			return null;
		}
		if (attribute.check(o)) {
			return attribute.castTo(o);
		}
		throw new AttributeTypeException();
	}
	
	public Object getValue(String attribute) {
		Object o = data.get(attribute);
		if (o!=null) {
			return o;
		}
		return null;
		
	}

	
	public Attribute<?> getAttribute(String n) {
		return attributes.get(n);

	}

	public  void setValue(Attribute<?> attribute, Object value)  throws AttributeTypeException {
		_setValue( attribute , value );
	}
	
	private void _setValue(Attribute<?> attribute, Object value) throws AttributeTypeException {
		if (!attribute.isValidValue(value)) {
			throw new AttributeTypeException();
		}
		if (isConsistent(attribute)) {
			data.put(attribute.getName(), value);
		} else {
			throw new AttributeTypeException();
		}
	}

	public void setValue(String attribute, Object value) throws AttributeTypeException {
		if (!attributes.get(attribute).isValidValue(value)) {
			throw new AttributeTypeException();
		}
		if (isConsistent(attributes.get(attribute))) {
			data.put(attribute, value);
		} else {
			throw new AttributeTypeException();
		}
	}

	@Override
	public String toString() {
		return data.toString();
	}

	public  boolean satisfy( Condition p ) throws AttributeTypeException {
		return p.isSatisfiedBy( this );
	}
	
	public  boolean waitUntil( Condition p ) throws AttributeTypeException, InterruptedException {
		while (!p.isSatisfiedBy(this)) {
		}
		return true;
	}

	public  void update(Store update) throws AttributeTypeException {
		for (Attribute<?> a : update.attributes.values()) {
			_setValue(a, update.getValue(a));
		}
	}
}
