package recipe.lang.types;

import org.petitparser.parser.Parser;
import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.TypingContext;

import java.util.HashMap;
import java.util.Map;

public class Guard extends Type {
    private TypedVariable[] parameters;
    private String label;

    //TODO this should be in TypingContext
    private static Map<String, Guard> existing = new HashMap<>();

    public static void clear(){
        existing.clear();
    }

    public static Guard getGuardDefinition(String name) throws Exception {
        if(existing.containsKey(name)) return existing.get(name);
        else throw new Exception("No such guard definition: " + name);
    }

    public Guard(String label, TypedVariable[] parameters) throws TypeCreationException{
        if(parameters == null){
            throw new TypeCreationException("Guard type initialised with null.");
        }
        if(existing.containsKey(label)){
            throw new TypeCreationException("Guard with name " + label + " already exists.");
        }
        this.label = label;
        this.parameters = parameters;
        existing.put(label, this);
    }

    public TypedVariable[] getParameters(){
        return parameters;
    }

    @Override
    public Object interpret(String value) throws MismatchingTypeException {
        try{
            return valueParser().parse(value);
        } catch (Exception e) {
            throw new MismatchingTypeException(e.getMessage());
        }
    }

    @Override
    public String name() {
        return label;
    }

    @Override
    public Parser valueParser() throws Exception {
        TypingContext context = new TypingContext();
        for(TypedVariable v : parameters){
            context.set(v.getName(), v.getType());
        }

        Parser parser = Condition.parser(context);

        return parser;
    }
}
