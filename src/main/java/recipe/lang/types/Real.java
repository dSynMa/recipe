package recipe.lang.types;

import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.utils.TypingContext;

public class Real extends Number {
    private static Real base = new Real();
    protected Real(){}

    public static Real getType(){
        return base;
    }

    public org.petitparser.parser.Parser valueParser(){
        return (CharacterParser.digit().plus()).seq((CharacterParser.of('.').seq(CharacterParser.digit().plus().flatten())).flatten()).flatten()
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
            return Double.parseDouble(value);
        } catch (Exception e) {
            throw new MismatchingTypeException(value + " is not of type " + name());
        }
    }

    @Override
    public String name() {
        return "real";
    }
}
