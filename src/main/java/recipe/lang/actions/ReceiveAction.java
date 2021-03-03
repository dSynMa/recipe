package recipe.lang.actions;

import recipe.lang.agents.AgentBehaviour;
import recipe.lang.conditions.Condition;

public class ReceiveAction implements Action {
    public Condition psi;
    public String channel;
    public String message;
    public String update;

    public ReceiveAction(Condition psi, String channel, String message, String update) {
        this.psi = psi;
        this.channel = channel;
        this.message = message;
        this.update = update;
    }

    public String toString() {
        return psi.toString() + "#" + channel + "#" + "!" + "(" + message + ")[" + update + "]";
    }
}
