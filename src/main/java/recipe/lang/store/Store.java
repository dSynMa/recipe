package recipe.lang.store;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.strings.StringVariable;

public class Store {
    private HashMap<String, TypedValue> data;

	private HashMap<String, TypedVariable> attributes;

	public Map<String, TypedVariable> getAttributes() {
		return attributes;
	}

	public Store(Map<String, Expression> data, Map<String, TypedVariable> attributes) throws AttributeTypeException, AttributeNotInStoreException {
		this.data = new HashMap<>();

		for(String v : data.keySet()){
			this.data.put(v, data.get(v).valueIn(this));
		}

		this.attributes = new HashMap<>(attributes);
	}

	public Store() {
		this.data = new HashMap<>();
		this.attributes = new HashMap<>();
	}

	protected boolean safeAddAttribute(TypedVariable attribute) {
		TypedVariable old = attributes.get(attribute.getName());
		if (old == null) {
			attributes.put(attribute.getName(), attribute);
			return true;
		}
		return old.equals(attribute);
	}

	public TypedValue getValue(TypedVariable attribute) throws AttributeTypeException {
		TypedValue o = data.get(attribute.getName());
		if (o == null) {
			return null;
		}
		if (attribute.isValidValue(o)) {
			return o;
		}
		throw new AttributeTypeException();
	}
	
	public TypedValue getValue(String attribute) {
		TypedValue o = data.get(attribute);
		if (o != null) {
			return o;
		}
		return null;
	}

	public TypedVariable getAttribute(String n) throws AttributeNotInStoreException {
		if(!attributes.containsKey(n)){
			throw new AttributeNotInStoreException();
		}

		return attributes.get(n);
	}

	public  void setValue(TypedVariable attribute, TypedValue value)  throws AttributeTypeException {
		_setValue( attribute , value );
	}
	
	private void _setValue(TypedVariable attribute, TypedValue value) throws AttributeTypeException {
		if (!attribute.isValidValue(value)) {
			throw new AttributeTypeException();
		}
		if (safeAddAttribute(attribute)) {
			data.put(attribute.getName(), value);
		} else {
			throw new AttributeTypeException();
		}
	}

	public void setValue(String attribute, TypedValue value) throws AttributeTypeException, AttributeNotInStoreException {
		if(!attributes.containsKey(attribute)){
			throw new AttributeNotInStoreException();
		}

		if (!attributes.get(attribute).isValidValue(value)) {
			throw new AttributeTypeException();
		}

		data.put(attribute, value);
	}

	@Override
	public String toString() {
		return data.toString();
	}

	@Override
	public boolean equals(Object other){
		if(other.getClass().equals(Store.class)){
			Store otherStore = (Store) other;
			if(data.keySet().size() == otherStore.data.keySet().size()) {
				for (String key : data.keySet()) {
					if(!otherStore.data.containsKey(key) ||
							!otherStore.data.get(key).equals(data.get(key))){
						return false;
					}
				}

				if(attributes.keySet().size() == otherStore.attributes.keySet().size()) {
					for (String key : attributes.keySet()) {
						if(!otherStore.attributes.containsKey(key) ||
								!otherStore.attributes.get(key).equals(attributes.get(key))){
							return false;
						}
					}

					return true;
				}
			}
		}

		return false;
	}

	public boolean satisfy( Condition p ) throws AttributeTypeException, AttributeNotInStoreException {
		return p.isSatisfiedBy( this );
	}
	
	public boolean waitUntil( Condition p ) throws AttributeTypeException, InterruptedException, AttributeNotInStoreException {
		while (!p.isSatisfiedBy(this)) {
		}
		return true;
	}

	public void update(Store update) throws AttributeTypeException {
		for (TypedVariable a : update.attributes.values()) {
			_setValue(a, update.getValue(a));
		}
	}

	public Store copyWithRenaming(Function<String, String> renaming) throws AttributeTypeException, AttributeNotInStoreException {
		Store store = new Store();

		for(TypedVariable v : this.attributes.values()){
			String newName = renaming.apply(v.getName());
			if(v.getClass().equals(BooleanVariable.class)){
				store.safeAddAttribute(new BooleanVariable(newName));
			} else if(v.getClass().equals(StringVariable.class)){
				store.safeAddAttribute(new StringVariable(newName));
			} else if(v.getClass().equals(ChannelVariable.class)){
				store.safeAddAttribute(new ChannelVariable(newName));
			} else if(v.getClass().equals(NumberVariable.class)){
				store.safeAddAttribute(new NumberVariable(newName));
			}

			store.setValue(newName, (TypedValue) this.getValue(newName));
		}

		return store;
	}
}
