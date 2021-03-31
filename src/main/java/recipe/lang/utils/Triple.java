package recipe.lang.utils;



public class Triple<L, M, R> {

    private L left;
    private M middle;
    private R right;

    public Triple(L left, M middle, R right) {
        assert left != null;
        assert middle != null;
        assert right != null;

        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public M getMiddle() {
        return middle;
    }

    public R getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "(" + left + "," + middle + "," + right + ")";
    }

    public static void main(String[] arg) {
        Triple<String, String, String> pair = new Triple<>("left", "middle", "right");
        System.out.println(pair.toString());
    }
}