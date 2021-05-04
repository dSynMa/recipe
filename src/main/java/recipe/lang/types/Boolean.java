package recipe.lang.types;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

public class Boolean extends Type {
    static Boolean base = new Boolean();
    private Boolean(){}

    public static Boolean getType(){
        return base;
    }

    @Override
    public Object interpret(String value) throws MismatchingTypeException {
        try {
            return java.lang.Boolean.parseBoolean(value);
        } catch (Exception e){
            throw new MismatchingTypeException(value + " is not of type " + name());
        }
    }

    public org.petitparser.parser.Parser parser(){
        return StringParser.of("true")
                .or(StringParser.of("TRUE"))
                .or(StringParser.of("True"))
                .or(StringParser.of("false"))
                .or(StringParser.of("FALSE"))
                .or(StringParser.of("False"))
                        .map((String value) -> {
                            try {
                                return new TypedValue(this, value);
                            } catch (MismatchingTypeException e) {
                                e.printStackTrace();
                                return null;
                            }
                        });
    }

    @Override
    public String name() {
        return "boolean";
    }
}
