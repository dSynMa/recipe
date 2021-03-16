package recipe.lang.actions;


import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Store;

public class Message {

    private String channel;
	private Object value;
	private Condition predicate;
    


	public Message(String channel, Object value, Condition predicate) {
		this.channel=channel;
        this.value = value;
		this.predicate = predicate;
		
	}

	public boolean isAReceiverFor(Store store) {
		try {
			return predicate.isSatisfiedBy(store);
		} catch (AttributeTypeException | AttributeNotInStoreException e) {
			return false;
		}
	}

	public Object getValue() {
		return value;
	}

	public Condition getPredicate() {
		return predicate;
	}
	
	
	@Override
	public String toString() {
		return "Message [value=" + (value==null?"_":value) + ", predicate=" + predicate + "  ]";
	}

	@Override
	public int hashCode() {
		return (value==null?0:value.hashCode())+predicate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Message) {
			Message other = (Message) obj;
			return (value==null?other.value==null:value.equals(other.value))
					&&(predicate.equals(other.predicate));
		}
		return false;
	}


}