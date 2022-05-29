package recipe.lang.ltol.observations;

import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.types.Boolean;

public class BasicObservation extends Observation{
    Condition condition;
    public BasicObservation(Condition condition){
        this.condition = condition;
    }
    public Condition getCondition(){
        return condition;
    }
    public void setCondition(Condition condition){
        this.condition = condition;
    }
    public String toString(){
        return condition.toString();
    }
}
