package recipe.lang.store;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import recipe.lang.definitions.Definition;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.exception.AttributeTypeException;

public class Store {
//	private HashMap<String, TypedValue> data;

	private HashMap<String, TypedVariable> attributes;

	public Definition getDefinition(String label) {
		return definitions.get(label);
	}

	public void setDefinition(String label, Definition definition) {
		this.definitions.put(label, definition);
	}

	private HashMap<String, Definition> definitions = new HashMap<>();

	public Map<String, TypedVariable> getAttributes() {
		return attributes;
	}

	public Store(Map<String, TypedVariable> attributes) throws AttributeTypeException, AttributeNotInStoreException {
//		this.data = new HashMap<>();
//
//		for(String v : data.keySet()){
//			this.data.put(v, data.get(v).valueIn(this));
//		}

		this.attributes = new HashMap<>(attributes);
	}

	public Store() {
//		this.data = new HashMap<>();
		this.attributes = new HashMap<>();
	}

//	public HashMap<String, TypedValue> getData() {
//		return data;
//	}

	protected boolean safeAddAttribute(TypedVariable attribute) {
		TypedVariable old = attributes.get(attribute.getName());
		if (old == null) {
			attributes.put(attribute.getName(), attribute);
			return true;
		}
		return old.equals(attribute);
	}

//	public TypedValue getValue(TypedVariable attribute) throws AttributeTypeException {
//		TypedValue o = data.get(attribute.getName());
//		if (o == null) {
//			return null;
//		}
//		if (attribute.isValidValue(o)) {
//			return o;
//		}
//		throw new AttributeTypeException();
//	}
//
//	public TypedValue getValue(String attribute) {
//		TypedValue o = data.get(attribute);
//		if (o != null) {
//			return o;
//		}
//		return null;
//	}

	public TypedValue getValue(Object attribute) {
		//TODO
		return null;
	}

	public TypedVariable getAttribute(String n) throws AttributeNotInStoreException {
		if(!attributes.containsKey(n)){
			throw new AttributeNotInStoreException();
		}

		return attributes.get(n);
	}

//	public  void setValue(TypedVariable attribute, TypedValue value)  throws AttributeTypeException {
//		_setValue( attribute , value );
//	}
//
//	private void _setValue(TypedVariable attribute, TypedValue value) throws AttributeTypeException {
//		if (!attribute.isValidValue(value)) {
//			throw new AttributeTypeException();
//		}
//		if (safeAddAttribute(attribute)) {
//			data.put(attribute.getName(), value);
//		} else {
//			throw new AttributeTypeException();
//		}
//	}

//	public void setValue(String attribute, TypedValue value) throws AttributeTypeException, AttributeNotInStoreException {
//		if(!attributes.containsKey(attribute)){
//			throw new AttributeNotInStoreException();
//		}
//
//		if (!attributes.get(attribute).isValidValue(value)) {
//			throw new AttributeTypeException();
//		}
//
//		data.put(attribute, value);
//	}

	@Override
	public String toString() {
		return attributes.toString();
	}

	@Override
	public boolean equals(Object other){
		if(other.getClass().equals(Store.class)){
			Store otherStore = (Store) other;
//			if(data.keySet().size() == otherStore.data.keySet().size()) {
//				for (String key : data.keySet()) {
//					if(!otherStore.data.containsKey(key) ||
//							!otherStore.data.get(key).equals(data.get(key))){
//						return false;
//					}
//				}

				if(attributes.keySet().size() == otherStore.attributes.keySet().size()) {
					for (String key : attributes.keySet()) {
						if(!otherStore.attributes.containsKey(key) ||
								!otherStore.attributes.get(key).equals(attributes.get(key))){
							return false;
						}
					}

					return true;
				}
//			}
		}

		return false;
	}

	public boolean satisfy( Condition p ) throws AttributeTypeException, AttributeNotInStoreException, MismatchingTypeException {
		return p.isSatisfiedBy( this );
	}
	
	public boolean waitUntil( Condition p ) throws AttributeTypeException, InterruptedException, AttributeNotInStoreException, MismatchingTypeException {
		while (!p.isSatisfiedBy(this)) {
		}
		return true;
	}

	public void update(Store update) throws AttributeTypeException {
		for (TypedVariable a : update.attributes.values()) {
			safeAddAttribute(a);
		}
	}

	public Store copyWithRenaming(Function<String, String> renaming) throws AttributeTypeException, AttributeNotInStoreException, RelabellingTypeException {
		Store store = new Store();

		for(TypedVariable v : this.attributes.values()){
			String newName = renaming.apply(v.getName());
			TypedVariable newV = (TypedVariable) v.relabel(tv -> ((TypedVariable) tv).sameTypeWithName(newName));
			store.safeAddAttribute(newV);

//			store.setValue(newName, (TypedValue) this.getValue(newName));
		}

		return store;
	}
}
