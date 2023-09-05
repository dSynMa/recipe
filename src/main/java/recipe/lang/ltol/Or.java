package recipe.lang.ltol;

import recipe.lang.expressions.TypedVariable;
import recipe.lang.utils.Triple;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Or extends LTOL{
    LTOL left;
    LTOL right;
    public Or(LTOL left, LTOL right){
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString(){
        return "(" + left.toString() + " | " + right.toString() + ")";
    }

    public boolean isPureLTL() {
        return left.isPureLTL() && right.isPureLTL();
    }    public Triple<Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) throws Exception {
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> leftOut = left.abstractOutObservations(counter);
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> rightOut = right.abstractOutObservations(leftOut.getLeft());
        Map<String, Observation> union = new HashMap<>(leftOut.getMiddle());
        union.putAll(rightOut.getMiddle());

        return new Triple<>(rightOut.getLeft(), union, new Or(leftOut.getRight(), rightOut.getRight()));
    }

    public LTOL rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Or(left.rename(relabelling), right.rename(relabelling));
    }

    public LTOL toLTOLwithoutQuantifiers() throws Exception {
        return new Or(left.toLTOLwithoutQuantifiers(), right.toLTOLwithoutQuantifiers());
    }
}
