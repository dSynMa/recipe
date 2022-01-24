package recipe.lang.exception;

public class MismatchingTypeException extends Exception{
    private static final long serialVersionUID = 7L;
    public MismatchingTypeException(String message){
        super(message);
    }
}
