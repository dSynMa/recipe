package recipe.lang.agents;

import recipe.lang.conditions.Condition;

public class Guarded extends Agent {
    public Condition guard;
    public Agent a;

    public Guarded(Condition guard, Agent a) {
        this.guard = guard;
        this.a = a;
    }
}
