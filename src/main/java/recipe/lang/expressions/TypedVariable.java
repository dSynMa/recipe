package recipe.lang.expressions;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.store.Store;
import recipe.lang.types.Type;

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
        return store.getValue(this);
    }

    @Override
    public Expression<T> close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
        if(!CV.contains(this.name)){
            return valueIn(store);
        } else{
            return this;
        }
    }

    @Override
    public Expression<T> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
        return relabelling.apply(this);
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
}