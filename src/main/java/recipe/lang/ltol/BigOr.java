package recipe.lang.ltol;

import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.Triple;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BigOr extends LTOL{
    List<TypedVariable> vars;
    LTOL ltol;

    public BigOr(TypedVariable var, LTOL ltol){
        vars = new ArrayList<>();
        vars.add(var);
        this.ltol = ltol;
    }

    public BigOr(List<TypedVariable> vars, LTOL ltol){
        this.vars = vars;
        this.ltol = ltol;
    }

    public String toString() {
        List<String> fmt = new ArrayList<String>(vars.size());
        for (TypedVariable variable : vars) {
            fmt.add(variable.toTypedString());
        }
        return "\\/" + String.join(" , ", fmt) + "." + "(" + ltol.toString() + ")";
    }

    public boolean isPureLTL() {
        return ltol.isPureLTL();
    }

    private LTOL asPureLTOL;

    @Override
    public Triple<Integer, Map<String, Observation>, LTOL> abstractOutObservations(Integer counter) throws Exception {
        if (asPureLTOL == null) {
            asPureLTOL = this.toLTOLwithoutQuantifiers();
        }

        return asPureLTOL.abstractOutObservations(counter);
    }
    @Override
    public LTOL rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new BigAnd(vars, ltol.rename((x) -> !vars.contains(x) ? relabelling.apply(x) : x));
    }

    public LTOL toLTOLwithoutQuantifiers() throws Exception {
        return rewriteOutBigOr(vars, ltol);
    }

    private static LTOL rewriteOutBigOr(List<TypedVariable> vars, LTOL ltol) throws Exception {
        Set<LTOL> possibleValues = LTOL.rewriteOutBigAndOr(vars, ltol);

        LTOL result = null;

        for(LTOL ltol1 : possibleValues){
            if(result == null){
                result = ltol1;
            } else{
                result = new Or(result, ltol1);
            }
        }

        return result;
    }
}
