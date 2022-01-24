package recipe.lang.types;

import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.utils.TypingContext;

import java.util.Objects;

public abstract class Type {
    public abstract Object interpret(String value) throws MismatchingTypeException;
    public abstract String name();
    public abstract org.petitparser.parser.Parser valueParser() throws Exception;
    public boolean isValidValue(Object value){
        try{
            this.interpret(value.toString());
            return true;
        } catch (Exception e){
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
