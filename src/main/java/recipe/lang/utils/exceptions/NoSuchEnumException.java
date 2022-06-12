package recipe.lang.utils.exceptions;

public class NoSuchEnumException extends Exception{
    private static final long serialVersionUID = 8L;
    public NoSuchEnumException(String message){
        super(message);
    }
}
