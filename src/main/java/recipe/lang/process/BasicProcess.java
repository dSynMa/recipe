package recipe.lang.process;

import recipe.lang.expressions.Expression;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Type;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class BasicProcess extends Process {
    protected String label;
    protected Expression<Boolean> psi;
    protected Expression<Enum> channel;
    protected Map<String, Expression> update;

    public String getLabel() {
        return label;
    }

    public abstract String prettyPrintLabel();

    public Expression<Boolean> getPsi() {
        return psi;
    }

    public void setPsi(Expression<Boolean> psi1) {
        psi = psi1;
    }

    public Expression<Enum> getChannel() {
        return channel;
    }

    public Map<String, Expression> getUpdate() {
        return update;
    }

    public Set<String> wantedData(Map<String, Type> msgStruct) {
        Set<String> result = new HashSet<>();
        for (Expression expression : update.values()) {
            for (Object sub : expression.subformulas()) {
                if (msgStruct.containsKey(sub.toString())) {
                    result.add(sub.toString());
                }
            }
        }
        return result;
    }
}
