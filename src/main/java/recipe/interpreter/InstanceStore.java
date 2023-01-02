package recipe.interpreter;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import recipe.lang.agents.Agent;
import recipe.lang.agents.State;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Number;
import recipe.lang.types.Type;
import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.MismatchingTypeException;

public class InstanceStore extends Store {
    private Map<TypedVariable, TypedValue> data;
    private State<Integer> state;

    @Override
    public String toString() {
        return this.data.toString();
    }

    public State<Integer> getState() {
        return state;
    }

    public TypedValue getValue(Object attribute) {
        // System.out.print("Looking for ");
        // System.out.print(attribute);
        // System.out.print(" in ");
        // System.out.print(this.data);
        // System.out.print("--->");
        TypedValue result = this.data.get(attribute);
        // System.out.println(result);
        return result;
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

    public InstanceStore(JSONObject obj, Agent agent) {
        data = new HashMap<TypedVariable, TypedValue>();
        state = new State<Integer>(
            agent.getName(),
            Integer.valueOf(obj.getString("state"))
        );

        obj.keySet().forEach(var -> {
            if (!var.toString().equals("state")) {
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
