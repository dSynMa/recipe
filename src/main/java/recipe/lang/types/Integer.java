package recipe.lang.types;

import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.MismatchingTypeException;

public class Integer extends Real {
    private static Integer base = new Integer();
    protected Integer(){
        super();
    }

    public static Integer getType(){
        return base;
    }

    public org.petitparser.parser.Parser parser(){
        return CharacterParser.digit().plus().flatten();
    }

    @Override
    public Double interpret(String value) throws MismatchingTypeException {
        try{
            return Double.valueOf(java.lang.Integer.parseInt(value.replaceAll(".0+$", "")));
        } catch (Exception e) {
            throw new MismatchingTypeException(value + " is not of type " + name());
        }
    }

    @Override
    public String name() {
        return "integer";
    }
}
