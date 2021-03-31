package recipe.lang.agents;

public class State<T> {
    public T label;

    public State(T label){
        this.label = label;
    }

    public T getLabel(){
        return label;
    }
}
