package recipe.lang.agents;

public class Choice extends Agent {
    public Agent a;
    public Agent b;

    public Choice(Agent a, Agent b) {
        this.a = a;
        this.b = b;
    }
}