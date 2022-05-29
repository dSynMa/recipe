package recipe.lang.ltol;

public class Eventually extends LTOL{
    LTOL ltol;
    public Eventually(LTOL ltol){
        this.ltol = ltol;
    }
    public LTOL getLTOL(){
        return ltol;
    }
    public String toString(){
        return "F(" + ltol + ")";
    }
    public boolean isPureLTL() {
        return ltol.isPureLTL();
    }
}