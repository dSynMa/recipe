package recipe.lang.store;

import java.util.ArrayList;
import java.util.List;

import recipe.Config;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;

public class CompositeStore extends Store {

    // private Store base;
    // private Store shadow;
    private List<Store> stack;
    private List<Boolean> isReceiverStore;
    private recipe.lang.System system;

    public CompositeStore(recipe.lang.System s) {
        this.stack = new ArrayList<Store>(2);
        this.isReceiverStore = new ArrayList<Boolean>();
        this.system = s;
    }

    public CompositeStore() { this(null); }

    public void pushReceiverStore(Store store) { push(store, true); }

    public void push(Store store) { push(store, false); }

    private void push(Store store, boolean isReceiver) {
        stack.add(store);
        isReceiverStore.add(isReceiver);
    }

    public Store pop() {
        Store s = stack.get(stack.size()-1);
        stack.remove(stack.size()-1);
        return s;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("CompositeStore {\n");
        for (Store store : stack) {
            result.append("    ");
            result.append(store.toString());
            result.append("\n");
        }
        result.append("}\n");
        return result.toString();
    }

    @Override
    public TypedValue getValue(Object attribute) {

        for (int i = stack.size()-1; i >= 0; i--) {
            TypedValue result = stack.get(i).getValue(attribute);
            Boolean isCV = false;
            if (system != null) {
                isCV = Config.isCvRef(system, attribute.toString());
            }
            if (result != null && (!isCV || isReceiverStore.get(i))) {
                return result;
            }
        }
        
        return null;
    }
}
