package recipe.lang.ltol;

public class Not extends LTOL{
    LTOL ltol;
    public Not(LTOL ltol){
        this.ltol = ltol;
    }

    public String toString(){
        return "!(" + ltol.toString() + ")";
    }

    public boolean isPureLTL() {
        return ltol.isPureLTL();
    }
}
