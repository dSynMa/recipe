package recipe.lang.process;

import java.util.Map;

import recipe.lang.expressions.Expression;

public abstract class BasicProcessWithMessage extends BasicProcess {
    public Map<String, Expression> message;
    
    public Map<String, Expression> getMessage() {
        return message;
    }
}
