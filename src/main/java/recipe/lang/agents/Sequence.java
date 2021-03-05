package recipe.lang.agents;

public class Sequence extends Agent {
    public Agent a;
    public Agent b;

    public Sequence(Agent a, Agent b) {
        this.a = a;
        this.b = b;
    }
}
