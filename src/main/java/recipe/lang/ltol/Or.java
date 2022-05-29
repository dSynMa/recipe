package recipe.lang.ltol;

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
    }
}
