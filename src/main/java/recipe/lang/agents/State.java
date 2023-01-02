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

    @Override
    public String toString(){
        return label.toString();
    }


    @Override
    public int hashCode(){
        int result = 37;
        if (this.agent != null) {
            result += this.agent.hashCode();
        }
        result *= 37;
        if (this.label != null) {
            result += this.label.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            State<T> other = (State<T>) obj;
            boolean result = this.label.equals(other.label) && this.agent.equals(other.agent);
            return result;
        } catch (ClassCastException ex) {
            return false;
        }        
        
    }
}
