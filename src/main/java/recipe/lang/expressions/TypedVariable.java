package recipe.lang.expressions;

import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;
import recipe.lang.store.Store;
import recipe.lang.types.Type;

import java.lang.Boolean;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class TypedVariable<T extends Type> implements Expression<T> {
    private String name;
    private T type;

    public TypedVariable(T type, String name) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public T getType() {
        return type;
    }

    public Boolean isValidValue(TypedValue val) {
        return type.isValidValue(val.getValue());
    }

    public TypedVariable<T> sameTypeWithName(String name){
        return new TypedVariable(type, name);
    }

    @Override
    public TypedValue<T> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {

        TypedValue<T> result = store.getValue(this);
        // System.err.printf("Evaluating %s on store %s yields %s\n", this, store, result);
        return result;
    }

    @Override
    public Expression<T> simplify() throws AttributeNotInStoreException, AttributeTypeException {
        return this;
    }

    @Override
    public Expression<T> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
        return relabelling.apply(this);
    }

    @Override
    public Boolean isValidAssignmentFor(TypedVariable var) {
        return var.getType().accepts(getType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypedVariable<?> that = (TypedVariable<?>) o;
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toNuXmv() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String toTypedString() {
        return name + " : " + type.name();
    }

    public Set<Expression<recipe.lang.types.Boolean>> subformulas(){
        Set<Expression<recipe.lang.types.Boolean>> subformulas = new HashSet<>();
        try {
            subformulas.add((Expression<recipe.lang.types.Boolean>) this);
        } catch (Exception e){
        }
        return subformulas;
    }

    public Expression<T> replace(java.util.function.Predicate<Expression<T>> cond,
                                 Function<Expression<T>, Expression<T>> act) {
        if (cond.test(this)) {
            return act.apply(this);
        } else {
            return this;
        }
    }

    public Expression<T> removePreds(){
        return this;
    }
}