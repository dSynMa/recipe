package recipe.lang.types;

import org.petitparser.parser.primitive.StringParser;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.expressions.predicate.Condition;

import java.util.HashSet;
import java.util.Set;

public class Boolean extends Type {
    static Boolean base = new Boolean();
    Boolean(){}

    public static Boolean getType(){
        return base;
    }

    @Override
    public Object interpret(String value) throws MismatchingTypeException {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new MismatchingTypeException(value + " is not of type " + name());
    }

    public org.petitparser.parser.Parser valueParser(){
        return (StringParser.ofIgnoringCase("true").seq(org.petitparser.parser.primitive.CharacterParser.word().not()).flatten()
                .map((String value) -> {
                    return Condition.getTrue();
                }))
                .or(StringParser.ofIgnoringCase("false").seq(org.petitparser.parser.primitive.CharacterParser.word().not()).flatten()
                        .map((String value) -> {
                            return Condition.getFalse();
                        }));
    }

    public Set getAllValues() {
        Set<String> values = new HashSet<>();
        values.add("true");
        values.add("false");
        return values;
    }
    @Override
    public String name() {
        return "boolean";
    }
}
