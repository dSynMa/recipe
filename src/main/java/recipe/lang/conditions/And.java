package recipe.lang.conditions;

public class And implements Condition{
    public Condition a;
    public Condition b;

    public And(Condition a, Condition b) {
        this.a = a;
        this.b = b;
    }
}
