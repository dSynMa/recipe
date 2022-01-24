package recipe.lang.types;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.Locale;

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

    public org.petitparser.parser.Parser valueParser(){
        return (StringParser.ofIgnoringCase("true")
                .map((String value) -> {
                    return Condition.getTrue();
                }))
                .or(StringParser.ofIgnoringCase("false")
                        .map((String value) -> {
                            return Condition.getFalse();
                        }));
    }

    @Override
    public String name() {
        return "boolean";
    }
}
