package recipe.lang.ltol.observations;

import recipe.lang.expressions.predicate.Condition;

public class ObsExists extends Observation{
    BasicObservation condition;

    public ObsExists(BasicObservation condition){
        this.condition = condition;
    }

    public BasicObservation getCondition(){
        return condition;
    }

    public String toString(){
        return "exists(" + condition.toString() + ")";
    }
}
