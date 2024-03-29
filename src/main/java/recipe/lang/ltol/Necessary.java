package recipe.lang.ltol;

import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Boolean;
import recipe.lang.utils.Triple;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Necessary extends LTOL{
    Observation obs;
    LTOL value;

    public Necessary(Observation obs, LTOL value) {
        this.obs = obs;
        this.value = value;
    }

    public Observation getObservation() {
        return obs;
    }

    public LTOL getValue() {
        return value;
    }

    public String toString() {
        return "<" + obs + ">(" + value + ")";
    }

    public boolean isPureLTL() {
        return false;
    }

    public Triple<java.lang.Integer, Map<String, Observation>, LTOL> abstractOutObservations(java.lang.Integer counter) throws Exception {
        Observation oldObs = new Observation(obs.observation);
        Map map = new HashMap<String, Observation>();
        map.put("obs" + counter, new Observation(oldObs.observation));

        TypedVariable var = new TypedVariable(Boolean.getType(), "obs" + counter);
        Triple<java.lang.Integer, Map<String, Observation>, LTOL> newValue = this.value.abstractOutObservations(counter + 1);
        map.putAll(newValue.getMiddle());

        return new Triple<>(newValue.getLeft(), map, new Next(new And(new Atom(var), newValue.getRight())));
    }

    public LTOL rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException{
        return new Necessary(obs.rename(relabelling), value.rename(relabelling));
    }

    public LTOL toLTOLwithoutQuantifiers() throws RelabellingTypeException, InfiniteValueTypeException, MismatchingTypeException {
        return this;
    }

}
