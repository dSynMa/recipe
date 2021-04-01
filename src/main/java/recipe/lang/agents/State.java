package recipe.lang.agents;

public class State<T> {

    public String agent;
    public T label;

    public State(T label){
        this.label = label;
    }

    public State(String agent, T label){
        this.agent = agent;
        this.label = label;
    }

    public T getLabel(){
        return label;
    }

    public String getAgent(){
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }
}
