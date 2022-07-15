package recipe.lang.ltol;

import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.Triple;

import java.util.Map;

public class BigAnd extends LTOL{
    TypedVariable var;
    Condition condition;
    LTOL ltol;
    public BigAnd(TypedVariable var, Condition condition, LTOL ltol){
        this.condition = condition;
        this.var = var;
        this.ltol = ltol;
    }

    public String toString(){
        return "/\\" + var.toString() + "." + condition.toString() + "(" + ltol.toString() + ")";
    }

    //TODO
    public boolean isPureLTL() {
        return ltol.isPureLTL();
    }

    //TODO
    @Override
    public Triple<Integer, Map<String, Observation>, LTOL> abstractOutObservations(Integer counter) {
        return null;
    }
}
