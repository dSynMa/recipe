package recipe.lang.agents;

import recipe.lang.agents.behaviour.AgentBehaviour;
import recipe.lang.agents.behaviour.Pair;

import java.util.function.Function;

public class Agent<CV, LocalState> {
    private LocalState local;
    private AgentBehaviour agentBehaviour;
    //TODO rename
    private Function<Pair<LocalState, CV>, Object> relabelling;

    public Agent(LocalState local, Function<Pair<LocalState, CV>, Object> relabelling){
        this.local = local;
        this.relabelling = relabelling;
    }

    public void update(Function<LocalState, LocalState> update) {
        update.apply(local);
    }

    public LocalState local(){
        return local;
    }

    public Object valueOf(CV cv){
        return relabelling.apply(new Pair(local, cv));
    }
}
