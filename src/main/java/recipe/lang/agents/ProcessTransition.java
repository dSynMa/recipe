package recipe.lang.agents;

import recipe.lang.process.BasicProcess;

import java.util.HashSet;
import java.util.Set;

public class ProcessTransition extends Transition<BasicProcess>{
    // three attributes of transition: source,action,destination

    public ProcessTransition(State source, State destination, BasicProcess label) {
        super(source, destination, label);
    }

    public ProcessTransition(String agent, State source, State destination, BasicProcess label) {
        super(source, destination, label, agent);
    }
}
