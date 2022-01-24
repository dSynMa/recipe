package recipe.lang.types;

import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.expressions.TypedValue;

public class Integer extends Real {
    private static Integer base = new Integer();
    protected Integer(){
        super();
    }

    public static Integer getType(){
        return base;
    }

    public org.petitparser.parser.Parser valueParser(){
        return CharacterParser.digit().plus().flatten()
                .map((Object val) -> {
            try {
                return new TypedValue(this, (String) val);
            } catch (MismatchingTypeException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public java.lang.Number interpret(String value) throws MismatchingTypeException {
        try{
            return java.lang.Integer.parseInt(value.replaceAll(".0+$", ""));
        } catch (Exception e) {
            throw new MismatchingTypeException(value + " is not of type " + name());
        }
    }

    @Override
    public String name() {
        return "integer";
    }
}
