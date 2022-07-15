package recipe.lang.ltol;

import recipe.lang.utils.Triple;

import java.util.HashMap;
import java.util.Map;

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

    public Triple<java.lang.Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) {
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> leftOut = left.abstractOutObservations(counter);
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> rightOut = right.abstractOutObservations(leftOut.getLeft());
        Map<String, Observation> union = new HashMap<>(leftOut.getMiddle());
        union.putAll(rightOut.getMiddle());

        return new Triple<>(rightOut.getLeft(), union, new Until(leftOut.getRight(), rightOut.getRight()));
    }
}
