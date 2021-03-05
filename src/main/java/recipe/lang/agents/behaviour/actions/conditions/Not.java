package recipe.lang.agents.behaviour.actions.conditions;

public class Not implements Condition{
    public Condition a;

    public Not(Condition a){
        this.a = a;
    }
}
