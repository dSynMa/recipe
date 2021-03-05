package recipe.lang.agents.behaviour;

public class Choice extends AgentBehaviour {
    public AgentBehaviour a;
    public AgentBehaviour b;

    public Choice(AgentBehaviour a, AgentBehaviour b) {
        this.a = a;
        this.b = b;
    }
}