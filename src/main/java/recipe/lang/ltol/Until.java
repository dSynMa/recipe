package recipe.lang.ltol;

import recipe.lang.expressions.TypedVariable;
import recipe.lang.utils.Triple;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Until extends LTOL{
    LTOL left;
    LTOL right;

    public Until(LTOL left, LTOL right) {
        this.left = left;
        this.right = right;
    }

    public LTOL getLeft() {
        return left;
    }

    public LTOL getRight() {
        return right;
    }

    public String toString() {
        return "(" + left + " U " + right + ")";
    }

    public boolean isPureLTL() {
        return left.isPureLTL() && right.isPureLTL();
    }

    public Triple<java.lang.Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) throws InfiniteValueTypeException, MismatchingTypeException, RelabellingTypeException {
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> leftOut = left.abstractOutObservations(counter);
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> rightOut = right.abstractOutObservations(leftOut.getLeft());
        Map<String, Observation> union = new HashMap<>(leftOut.getMiddle());
        union.putAll(rightOut.getMiddle());

        return new Triple<>(rightOut.getLeft(), union, new Until(leftOut.getRight(), rightOut.getRight()));
    }

    public LTOL rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Until(left.rename(relabelling), right.rename(relabelling));
    }

    public LTOL toLTOLwithoutQuantifiers() throws RelabellingTypeException, InfiniteValueTypeException, MismatchingTypeException {
        return new Until(left.toLTOLwithoutQuantifiers(), right.toLTOLwithoutQuantifiers());
    }
}
