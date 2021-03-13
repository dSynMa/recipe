package recipe.lang.utils;



public class Pair<L, R> {

    private L left;
    private R right;

    public Pair(L left, R right) {
        assert left != null;
        assert right != null;

        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "(" + left + "," + right + ")";
    }

    public static void main(String[] arg) {
        Pair<String, String> pair = new Pair<>("left", "right");
        System.out.println(pair.toString());
    }
}