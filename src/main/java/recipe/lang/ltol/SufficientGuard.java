package recipe.lang.ltol;

import recipe.lang.ltol.observations.Observation;

public class SufficientGuard extends LTOL{
    Observation obs;
    LTOL value;

    public SufficientGuard(Observation obs, LTOL value) {
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
        return "[" + obs + "](" + value + ")";
    }

    public boolean isPureLTL() {
        return false;
    }
}
