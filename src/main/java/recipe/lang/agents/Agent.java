package recipe.lang.agents;

import java.util.Map;
import java.util.function.Function;

public abstract class Agent<T> {
    private T local;

    public void update(Function<T,T> update) {
        update.apply(local);
    }

    public T local(){
        return local;
    }

}
