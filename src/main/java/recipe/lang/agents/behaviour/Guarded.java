package recipe.lang.agents.behaviour;

import recipe.lang.agents.behaviour.actions.conditions.Condition;

public class Guarded extends AgentBehaviour {
    public Condition guard;
    public AgentBehaviour a;

    public Guarded(Condition guard, AgentBehaviour a) {
        this.guard = guard;
        this.a = a;
    }
}
