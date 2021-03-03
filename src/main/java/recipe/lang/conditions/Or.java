package recipe.lang.conditions;

public class Or implements Condition{
    public Condition a;
    public Condition b;

    public Or(Condition a, Condition b) {
        this.a = a;
        this.b = b;
    }
}
