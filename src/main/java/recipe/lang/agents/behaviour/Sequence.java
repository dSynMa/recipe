package recipe.lang.agents.behaviour;

public class Sequence extends AgentBehaviour {
    public AgentBehaviour a;
    public AgentBehaviour b;

    public Sequence(AgentBehaviour a, AgentBehaviour b) {
        this.a = a;
        this.b = b;
    }
}
