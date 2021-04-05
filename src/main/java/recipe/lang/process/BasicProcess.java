package recipe.lang.process;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.predicate.Condition;

import java.util.Map;

public abstract class BasicProcess extends Process{
    protected Condition psi;
    protected ChannelExpression channel;
    protected Map<String, Expression> update;

    public Condition getPsi() {
        return psi;
    }

    public ChannelExpression getChannel() {
        return channel;
    }

    public Map<String, Expression> getUpdate() {
        return update;
    }
}
