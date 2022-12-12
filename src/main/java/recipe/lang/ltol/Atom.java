package recipe.lang.ltol;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Boolean;
import recipe.lang.utils.Triple;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Atom extends LTOL{
    Expression<Boolean> condition;

    public Atom(Expression<Boolean> condition) {
        this.condition = condition;
    }

    public Expression<Boolean> getCondition() {
        return condition;
    }

    public void setCondition(Expression<Boolean> condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return condition.toString();
    }

    @Override
    public boolean isPureLTL() {
        return true;
    }

    @Override
    public Triple<Integer, Map<String, Observation>, LTOL> abstractOutObservations(Integer counter) {
        return new Triple<>(counter, new HashMap<>(), this);
    }

    public LTOL rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Atom(condition.relabel((x) -> (relabelling.apply(x))));
    }

    public LTOL toLTOLwithoutQuantifiers() throws RelabellingTypeException, InfiniteValueTypeException, MismatchingTypeException {
        return this;
    }
}
