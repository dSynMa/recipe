package recipe.lang.actions;

import recipe.lang.expressions.predicate.Condition;

public class SendAction extends Action {
    public Condition psi;
    public String channel;
    public String message;
    public String update;
    public Condition guard;

    public SendAction(Condition psi, String channel, String message, String update, Condition guard) {
        this.psi = psi;
        this.channel = channel;
        this.message = message;
        this.update = update;
        this.guard = guard;
    }

    public String toString() {
        return psi.toString() + "#" + channel + "#" + "!" + "(" + message + ")[" + update + "]<" + guard.toString();
    }
}
