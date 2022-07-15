package recipe.lang.expressions;

import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;
import recipe.lang.store.Store;
import recipe.lang.types.*;
import recipe.lang.types.Enum;
import recipe.lang.types.Integer;
import recipe.lang.types.Process;

import java.lang.Boolean;
import java.util.Objects;
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
        return store.getValue(this);
    }

    @Override
    public Expression<T> close() throws AttributeNotInStoreException, AttributeTypeException {
        return this;
    }

    @Override
    public Expression<T> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
        return relabelling.apply(this);
    }

    @Override
    public Boolean isValidAssignmentFor(TypedVariable var) {
        if(getType().getClass().equals(Enum.class) && var.getType().getClass().equals(Enum.class)){
            return getType().equals(var.getType());
        } else if(getType().getClass().equals(Guard.class) && var.getType().getClass().equals(Guard.class)){
            return getType().equals(var.getType());
        } else if(getType().getClass().equals(BoundedInteger.class) && var.getType().getClass().equals(Integer.class)) {
            return Boolean.TRUE;
        } else if(getType().getClass().equals(Process.class) && var.getType().getClass().equals(Process.class)) {
            return getType().equals(var.getType());
        } else{
            return this.getType().getClass().isAssignableFrom(var.getType().getClass());
        }
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
    public String toString() {
        return name;
    }
}