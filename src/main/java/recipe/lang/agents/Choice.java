package recipe.lang.agents;

public class Choice implements AgentBehaviour{
    public AgentBehaviour a;
    public AgentBehaviour b;

    public Choice(AgentBehaviour a, AgentBehaviour b) {
        this.a = a;
        this.b = b;
    }
}