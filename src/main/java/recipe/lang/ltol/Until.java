package recipe.lang.ltol;

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
}
