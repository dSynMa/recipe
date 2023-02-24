package recipe.lang.ltol;

import recipe.lang.expressions.TypedVariable;
import recipe.lang.utils.Triple;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.Map;
import java.util.function.Function;

public class Not extends LTOL{
    LTOL ltol;
    public Not(LTOL ltol){
        this.ltol = ltol;
    }

    public String toString(){
        return "!(" + ltol.toString() + ")";
    }

    public boolean isPureLTL() {
        return ltol.isPureLTL();
    }

    public Triple<Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) throws Exception {
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> out = ltol.abstractOutObservations(counter);

        return new Triple<>(out.getLeft(), out.getMiddle(), new Not(out.getRight()));
    }

    public LTOL rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Not(ltol.rename(relabelling));
    }

    public LTOL toLTOLwithoutQuantifiers() throws Exception {
        return new Not(ltol.toLTOLwithoutQuantifiers());
    }
}
