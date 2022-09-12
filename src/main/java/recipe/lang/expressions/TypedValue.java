package recipe.lang.expressions;

import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Type;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public class TypedValue<T extends Type> implements Expression<T> {
    private T type;
    private Object value;

    public TypedValue(T type, String value) throws MismatchingTypeException {
        this.type = type;
        this.value = type.interpret(value);
    }

    public Object getValue(){
        return value;
    }

    @Override
    public TypedValue<T> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        return this;
    }

    @Override
    public Expression<T> simplify() throws AttributeNotInStoreException, AttributeTypeException {
        return this;
    }

    @Override
    public Expression<T> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
        return this;
    }

    @Override
    public java.lang.Boolean isValidAssignmentFor(TypedVariable var) {
        return var.getType().isValidValue(this.getValue());
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypedValue<?> that = (TypedValue<?>) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        if(type.equals(Boolean.getType())){
            return value.toString().toUpperCase(Locale.ROOT);
        }

        return value.toString();
    }
}
