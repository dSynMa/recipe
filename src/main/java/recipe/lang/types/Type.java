package recipe.lang.types;

import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;

import java.util.Objects;
import java.util.Set;

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

    public abstract Set getAllValues() throws InfiniteValueTypeException, MismatchingTypeException;

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
