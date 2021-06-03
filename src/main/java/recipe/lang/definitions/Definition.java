package recipe.lang.definitions;

import org.petitparser.parser.Parser;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;

public abstract class Definition {
    private String name;
    private TypedVariable[] parameters;
    private Expression expression;

    public Definition(String name, TypedVariable[] parameters, Expression expression) {
        this.name = name;
        this.parameters = parameters;
        this.expression = expression;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypedVariable[] getParameters() {
        return parameters;
    }

    public void setParameters(TypedVariable[] parameters) {
        this.parameters = parameters;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

}
