package recipe.lang.ltol;

import recipe.lang.expressions.TypedVariable;
import recipe.lang.utils.Triple;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.Map;
import java.util.function.Function;

public class Globally extends LTOL{
    LTOL ltol;
    public Globally(LTOL ltol){
        this.ltol = ltol;
    }
    public LTOL getLTOL(){
        return ltol;
    }
    public String toString(){
        return "G(" + ltol + ")";
    }
    public boolean isPureLTL() {
        return ltol.isPureLTL();
    }
    public Triple<Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) throws InfiniteValueTypeException, MismatchingTypeException, RelabellingTypeException {
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> out = ltol.abstractOutObservations(counter);

        return new Triple<>(out.getLeft(), out.getMiddle(), new Globally(out.getRight()));
    }

    public LTOL rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Globally(ltol.rename(relabelling));
    }

    public LTOL toLTOLwithoutQuantifiers() throws RelabellingTypeException, InfiniteValueTypeException, MismatchingTypeException {
        return new Globally(ltol.toLTOLwithoutQuantifiers());
    }
}