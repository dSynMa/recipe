package recipe.lang.ltol;

public class Globally extends LTOL{
    LTOL ltol;
    public Globally(LTOL ltol){
        this.ltol = ltol;
    }
    public LTOL getLTOL(){
        return ltol;
    }
    public String toString(){
        return "G(" + ltol + ")";
    }
    public boolean isPureLTL() {
        return ltol.isPureLTL();
    }
}