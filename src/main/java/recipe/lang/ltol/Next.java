package recipe.lang.ltol;

public class Next extends LTOL{
    LTOL ltol;
    public Next(LTOL ltol){
        this.ltol = ltol;
    }
    public LTOL getLTOL(){
        return ltol;
    }
    public String toString(){
        return "X(" + ltol + ")";
    }
    public boolean isPureLTL() {
        return ltol.isPureLTL();
    }
}
