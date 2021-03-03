package recipe.lang.agents;

public class Sequence implements AgentBehaviour{
    public AgentBehaviour a;
    public AgentBehaviour b;

    public Sequence(AgentBehaviour a, AgentBehaviour b) {
        this.a = a;
        this.b = b;
    }
}
