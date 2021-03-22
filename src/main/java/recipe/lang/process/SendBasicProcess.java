package recipe.lang.process;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.predicate.Condition;

import java.util.Map;

public class SendBasicProcess extends BasicProcess {
    public Condition psi;
    public ChannelExpression channel;
    public Map<String, Expression> message;
    public Map<String, Expression> update;
    public Condition guard;

    public SendBasicProcess(Condition psi, ChannelExpression channel, Map<String, Expression> message, Map<String, Expression> update, Condition guard) {
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
