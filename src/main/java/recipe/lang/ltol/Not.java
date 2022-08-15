package recipe.lang.ltol;

import recipe.lang.utils.Triple;

import java.util.Map;

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

    public Triple<Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) {
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> out = ltol.abstractOutObservations(counter);

        return new Triple<>(out.getLeft(), out.getMiddle(), new Not(out.getRight()));
    }
}