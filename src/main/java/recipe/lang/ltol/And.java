package recipe.lang.ltol;

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
}
