package recipe.lang.conditions;

public class Not implements Condition{
    public Condition a;

    public Not(Condition a){
        this.a = a;
    }
}
