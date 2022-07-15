package recipe.lang.ltol;

import recipe.lang.utils.Triple;

import java.util.Map;

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
    public Triple<Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) {
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> out = ltol.abstractOutObservations(counter);

        return new Triple<>(out.getLeft(), out.getMiddle(), new Eventually(out.getRight()));
    }
}