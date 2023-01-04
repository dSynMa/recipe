package recipe.lang.store;

import java.util.ArrayList;
import java.util.List;
import recipe.lang.expressions.TypedValue;

public class CompositeStore extends Store {

    // private Store base;
    // private Store shadow;
    private List<Store> stack;

    public CompositeStore() {
        this.stack = new ArrayList<Store>(2);
    }

    public void push(Store store) {
        stack.add(store);
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
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
