package recipe.lang.expressions;

import recipe.lang.expressions.predicate.Condition;
import recipe.lang.store.Store;
import recipe.lang.types.Enum;
import recipe.lang.types.Integer;
import recipe.lang.types.Process;
import recipe.lang.types.*;
import recipe.lang.utils.exceptions.*;

import java.lang.Boolean;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class Predicate implements Expression<recipe.lang.types.Boolean> {
    private String name;

    public Expression<recipe.lang.types.Boolean> getInput() {
        return input;
    }

    private Expression<recipe.lang.types.Boolean> input;
    private recipe.lang.types.Boolean type = recipe.lang.types.Boolean.getType();

    public Predicate(String name, Expression<recipe.lang.types.Boolean> input) {
        this.name = name;
        this.input = input;
    }

    public String getName() {
        return name;
    }

    public recipe.lang.types.Boolean getType() {
        return type;
    }

    public Boolean isValidValue(TypedValue val) {
        return type.isValidValue(val.getValue());
    }

    @Override
    public TypedValue<recipe.lang.types.Boolean> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, NotImplementedYetException, MismatchingTypeException {
        // throw new NotImplementedYetException("Predicate.valueIn not implemented yet");
        return input.valueIn(store);
    }

    @Override
    public Expression<recipe.lang.types.Boolean> simplify() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, RelabellingTypeException, MismatchingTypeException {
        return new Predicate(name, input.simplify());
    }

    @Override
    public Expression<recipe.lang.types.Boolean> relabel(java.util.function.Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Predicate(name, input.relabel(relabelling));
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
        Predicate that = (Predicate) o;
        return name.equals(that.name) && type.equals(that.type) && input.equals(that.input);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, input);
    }

    @Override
    public String toString() {
        return name + "(" + input.toString() + ")";
    }


    public Set<Expression<recipe.lang.types.Boolean>> subformulas(){
        Set<Expression<recipe.lang.types.Boolean>> subformulas = new HashSet<>();
        subformulas.add(this);
        return subformulas;
    }

    public Expression<recipe.lang.types.Boolean> replace(java.util.function.Predicate<Expression<recipe.lang.types.Boolean>> cond,
                                                         Function<Expression<recipe.lang.types.Boolean>, Expression<recipe.lang.types.Boolean>> act) {
        if (cond.test(this)) {
            return act.apply(this);
        } else {
            return new Predicate(name, input.replace(cond, act));
        }
    }

    public Expression<recipe.lang.types.Boolean> removePreds(){
        return this.input;
    }
}