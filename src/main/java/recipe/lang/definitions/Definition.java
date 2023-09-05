package recipe.lang.definitions;

import recipe.lang.expressions.TypedVariable;

public abstract class Definition<T> {
    private String name;
    private TypedVariable[] parameters;
    private T template;

    public Definition(String name, TypedVariable[] parameters, T expression) {
        this.name = name;
        this.parameters = parameters;
        this.template = expression;
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

    public T getTemplate() {
        return template;
    }

    public void setTemplate(T template) {
        this.template = template;
    }

}
