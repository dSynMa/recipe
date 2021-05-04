package recipe.lang.process;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;

import java.util.Map;

public abstract class BasicProcess extends Process{
    protected Expression<Boolean> psi;
    protected Expression<Enum> channel;
    protected Map<String, Expression> update;

    public Expression<Boolean> getPsi() {
        return psi;
    }

    public Expression<Enum> getChannel() {
        return channel;
    }

    public Map<String, Expression> getUpdate() {
        return update;
    }
}
