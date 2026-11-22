package recipe.lang.types;

import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;

import java.util.Objects;
import java.util.Set;

public abstract class Type {
    public abstract Object interpret(String value) throws MismatchingTypeException;
    public abstract String name();
    public abstract org.petitparser.parser.Parser valueParser() throws Exception;
    public boolean isValidValue(Object value){
        try{
            this.interpret(value.toString());
            return true;
        } catch (Exception e){
            return false;
        }
    }

    /** Whether every value of the source type can be assigned to this type. */
    public boolean accepts(Type source) {
        if (source instanceof UnionType) {
            return ((UnionType) source).getTypes().stream().allMatch(this::accepts);
        }
        if (this instanceof UnionType) {
            return ((UnionType) this).getTypes().stream().anyMatch(member -> member.accepts(source));
        }
        if (this instanceof BoundedInteger) {
            if (!(source instanceof BoundedInteger)) return false;
            BoundedInteger target = (BoundedInteger) this;
            BoundedInteger bounds = (BoundedInteger) source;
            return bounds.getMin() >= target.getMin() && bounds.getMax() <= target.getMax();
        }
        if (this instanceof Integer) return source instanceof Integer;
        if (this instanceof Real) return source instanceof Real;
        return equals(source);
    }

    public abstract Set getAllValues() throws InfiniteValueTypeException, MismatchingTypeException;

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
