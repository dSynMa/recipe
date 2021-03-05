package recipe.lang.agents.behaviour;

import recipe.lang.agents.behaviour.actions.conditions.Condition;

public class Guarded extends Agent {
    public Condition guard;
    public Agent a;

    public Guarded(Condition guard, Agent a) {
        this.guard = guard;
        this.a = a;
    }
}
