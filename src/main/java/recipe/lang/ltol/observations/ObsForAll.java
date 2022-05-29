package recipe.lang.ltol.observations;

import recipe.lang.expressions.predicate.Condition;

public class ObsForAll extends Observation{
    BasicObservation condition;

    public ObsForAll(BasicObservation condition){
        this.condition = condition;
    }

    public BasicObservation getCondition(){
        return condition;
    }

    public String toString(){
        return "forall(" + condition.toString() + ")";
    }
}
