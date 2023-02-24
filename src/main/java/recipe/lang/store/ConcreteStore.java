package recipe.lang.store;

import java.util.HashMap;
import java.util.Map;

import javax.management.AttributeNotFoundException;

import org.json.JSONObject;

import recipe.lang.agents.Agent;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.agents.State;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Boolean;
import recipe.lang.types.Number;
import recipe.lang.types.Type;
import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.NotImplementedYetException;

public class ConcreteStore extends Store {
    private Map<TypedVariable, TypedValue> data;
    private State state;
    private Agent agent;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s@%s: {", this.agent, this.state));
        for (TypedVariable v : this.data.keySet()) {
            builder.append(String.format("%s : %s =%s, ", v.getName(), v.getType(), data.get(v)));
        }
        builder.delete(builder.length()-2, builder.length()-1);
        builder.append("}");

        return builder.toString();
        // return  String.format(, this.data.toString());
    }

    public Map<TypedVariable, TypedValue> getData() { return data; }

    public State getState() {
        return state;
    }

    public void setState(State s) {
        state = s;
    }

    // Use sparingly.
    public void put(TypedVariable variable, TypedValue value) {
        this.data.put(variable, value);
    }

    public ConcreteStore BuildNext(ProcessTransition transition) throws AttributeNotInStoreException, AttributeTypeException, AttributeNotFoundException, MismatchingTypeException, NotImplementedYetException { 
        return this.BuildNext(transition, null);
    }

    public ConcreteStore BuildNext(ProcessTransition transition, Store messageStore) throws AttributeNotInStoreException, AttributeTypeException, AttributeNotFoundException, MismatchingTypeException, NotImplementedYetException {
        ConcreteStore newStore = new ConcreteStore(this, false);
        CompositeStore evaluationStore = new CompositeStore();
        evaluationStore.push(this);
        if (messageStore != null) {
            evaluationStore.push(messageStore);
        }
        newStore.setState(transition.getDestination());
        for (String varName : transition.getLabel().getUpdate().keySet()) {
            TypedValue varValue = transition.getLabel().getUpdate().get(varName).valueIn(evaluationStore);
            TypedVariable var = this.agent.getStore().getAttribute(varName);
            newStore.put(var, varValue);
        }
        return newStore;
    }

    public TypedValue getValue(Object attribute) {
        try {
            TypedVariable attr = (TypedVariable) attribute;
            // System.err.printf("getValue of %s\n", attr.getName());
            // If attribute is a cv, evaluate its corresponding expression
            // TODO check w/ Shaun if we can improve this (it's the @'s fault)
            
            if (this.agent != null && attr.getName().startsWith("@")) {
                for (TypedVariable cv : this.agent.getRelabel().keySet()) {
                    if (("@" + cv.getName()).equals(attr.getName())) {
                        // System.err.printf("match!\n");
                        TypedValue result = this.agent.getRelabel().get(cv).valueIn(this);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
            // Otherwise lookup variable and return its value
            TypedValue value = this.data.get(attr);
            if (value != null) {
                return value;
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println(e.getStackTrace());
        }
        // System.out.printf(">>> Lookup failed for %s in %s\n", attribute, this);
        return null;
	}

    private TypedValue typedValueFactory(Type t, String val) throws MismatchingTypeException {
        if (t instanceof Boolean) {
            return new TypedValue<Boolean>((Boolean) t, val);
        } else if (t instanceof Number) {
            return new TypedValue<Number>((Number) t, val);
        } else if (t instanceof recipe.lang.types.Enum) {
            return new TypedValue<recipe.lang.types.Enum>((recipe.lang.types.Enum) t, val);
        }

        else {
            throw new MismatchingTypeException("Unknown type " + t.toString());
        }
        // return new TypedValue<Boolean>((Boolean) t, val);

    }

    public Store push(TypedVariable variable, TypedValue value) {
        ConcreteStore top = new ConcreteStore(this, true);
        top.data.put(variable, value);
        return this.push(top);
    }

    public Store push(Map<TypedVariable, TypedValue> map) {
        ConcreteStore top = new ConcreteStore(this, true);
        top.data = map;
        return this.push(top);
    }

    public Store push(Store s) {
        CompositeStore result = new CompositeStore();
        result.push(this);
        result.push(s);
        return result;
    }

    protected ConcreteStore(ConcreteStore copy, boolean resetData) {
        this.agent = copy.agent;
        this.state = copy.state;
        this.data = new HashMap<TypedVariable, TypedValue>();
        if (!resetData) {
            copy.data.forEach((var, val) -> this.data.put(var, val));
        }
    }

    protected ConcreteStore() {}

    // Use sparingly.
    public ConcreteStore(Map<TypedVariable, TypedValue> data) { this.data = data; }

    public ConcreteStore(JSONObject obj, Agent agent) {
        this.agent = agent;
        data = new HashMap<TypedVariable, TypedValue>();
        state = new State<Integer>(
            agent.getName(),
            Integer.valueOf(obj.getString("automaton-state"))
        );

        obj.keySet().forEach(var -> {
            if (!var.toString().equals("automaton-state")) {
                try {
                    TypedVariable v = agent.getStore().getAttribute(var);
                    if (v != null) {
                        TypedValue val = typedValueFactory(v.getType(), obj.getString(var));
                        this.data.put(v, val);
                        // System.out.println(v.getName());
                        // // System.out.println(v.getType());
                        // System.out.println(var);
                        // System.out.println(val.getValue());
                        // System.out.println(val.getType());
                    }
                } catch (AttributeNotInStoreException x) {
                    System.out.println(var + " not found");
                } catch (MismatchingTypeException x) {
                    System.out.println(var + " throws MismatchingTypeException");
                }
            }
        });
    }

}
