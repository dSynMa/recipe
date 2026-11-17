package recipe.lang.expressions;

import recipe.lang.types.UnionType;
import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Type;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.math.BigDecimal;

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
        return fits(var.getType());
    }

    private boolean fits(Type target) {
        if (value instanceof TypedValue) {
            return ((TypedValue<?>) value).fits(target);
        }
        if (target instanceof UnionType) {
            return ((UnionType) target).getTypes().stream().anyMatch(this::fits);
        }
        // Numeric literals can fit a narrower domain even when their declared type does not.
        boolean numeric = type instanceof recipe.lang.types.Number
                && target instanceof recipe.lang.types.Number;
        return (numeric || target.accepts(type)) && target.isValidValue(value);
    }

    public T getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypedValue<?> that = (TypedValue<?>) o;

        if(this.type.getClass().equals(UnionType.class)){
            if(!that.getType().getClass().equals(UnionType.class)){
                return this.value.equals(that);
            } else{
                return this.value.equals(that.value);
            }
        } else if(that.getType().getClass().equals(UnionType.class)){
            return that.value.equals(this);
        }

        return equalityValue().equals(that.equalityValue());
    }

    private Object equalityValue() {
        if (value instanceof BigDecimal || (value instanceof java.lang.Number
                && Double.isFinite(((java.lang.Number) value).doubleValue()))) {
            return new BigDecimal(value.toString()).stripTrailingZeros();
        }
        return value;
    }

    @Override
    public int hashCode() {
        // Union values delegate equality to their enclosed TypedValue.
        if (value instanceof TypedValue) return value.hashCode();
        return Objects.hash(equalityValue());
    }

    @Override
    public String toString() {
        if(type.equals(Boolean.getType())){
            return value.toString().toUpperCase(Locale.ROOT);
        }

        if (value instanceof BigDecimal) {
            String decimal = ((BigDecimal) value).toPlainString();
            // Keep a real literal even when the value is integral (not integer division in SMV).
            return decimal.contains(".") ? decimal : decimal + ".0";
        }
        return value.toString();
    }

    public Set<Expression<Boolean>> subformulas(){
        return new HashSet<>();
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

    @Override
    public String toNuXmv() {
        return recipe.Config.broadcast.equals(value.toString()) ? recipe.Config.nuxmvBroadcast : toString();
    }

}
