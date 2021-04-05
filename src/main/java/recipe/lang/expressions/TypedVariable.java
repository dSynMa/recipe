package recipe.lang.expressions;

public interface TypedVariable extends Expression {
    String getName();
    Boolean isValidValue(TypedValue val);
    TypedVariable sameTypeWithName(String name);
}