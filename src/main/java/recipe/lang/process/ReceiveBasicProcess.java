package recipe.lang.process;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.predicate.Condition;

import java.util.Map;

public class ReceiveBasicProcess extends BasicProcess {
    public Condition psi;
    public ChannelExpression channel;
    public Map<String, Expression> message;
    public Map<String, Expression> update;

    public ReceiveBasicProcess(Condition psi, ChannelExpression channel, Map<String, Expression> message, Map<String, Expression> update) {
        this.psi = psi;
        this.channel = channel;
        this.message = message;
        this.update = update;
    }

    public String toString() {
        return psi.toString() + "#" + channel + "#" + "!" + "(" + message + ")[" + update + "]";
    }
}
