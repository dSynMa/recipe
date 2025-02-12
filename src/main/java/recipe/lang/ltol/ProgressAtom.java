package recipe.lang.ltol;

import java.util.function.Function;

import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

public class ProgressAtom extends Atom {

    public ProgressAtom() {
        super(Condition.getTrue());
    }

    @Override
    public String toString(){
        return "progress";
    }

    @Override
    public LTOL rename(Function<TypedVariable, TypedVariable> relabelling)
            throws RelabellingTypeException, MismatchingTypeException {
        return this;
    }

}
