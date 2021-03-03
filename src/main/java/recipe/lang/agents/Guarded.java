package recipe.lang.agents;

import recipe.lang.conditions.Condition;

public class Guarded implements AgentBehaviour{
    public Condition guard;
    public AgentBehaviour a;

    public Guarded(Condition guard, AgentBehaviour a) {
        this.guard = guard;
        this.a = a;
    }
}
