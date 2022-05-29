package recipe.lang.ltol;

import recipe.lang.expressions.predicate.Condition;

public class Atom extends LTOL{
    Condition condition;

    public Atom(Condition condition) {
        this.condition = condition;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return condition.toString();
    }

    @Override
    public boolean isPureLTL() {
        return true;
    }
}
