package recipe.lang.ltol;

import recipe.lang.utils.Triple;

import java.util.HashMap;
import java.util.Map;

public class And extends LTOL{
    LTOL left;
    LTOL right;
    public And(LTOL left, LTOL right){
        this.left = left;
        this.right = right;
    }

    public String toString(){
        return "(" + left.toString() + " & " + right.toString() + ")";
    }

    public boolean isPureLTL() {
        return left.isPureLTL() && right.isPureLTL();
    }

    public Triple<Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) {
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> leftOut = left.abstractOutObservations(counter);
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> rightOut = right.abstractOutObservations(leftOut.getLeft());
        Map<String, Observation> union = new HashMap<>(leftOut.getMiddle());
        union.putAll(rightOut.getMiddle());

        return new Triple<>(rightOut.getLeft(), union, new And(leftOut.getRight(), rightOut.getRight()));
    }
}
