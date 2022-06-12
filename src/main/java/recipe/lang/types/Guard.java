package recipe.lang.types;

import org.petitparser.parser.Parser;
import recipe.lang.definitions.GuardDefinition;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.TypeCreationException;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.TypingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Guard extends Type {
    private TypedVariable[] parameters;
    private String label;
    private static Map<String, GuardDefinition> definitions = new HashMap<>();
    public static void clear(){
        definitions.clear();
    }

    public static Set<String> definitionLabels(){
        return definitions.keySet();
    }

    public static void setDefinition(String label, GuardDefinition guardDefinition){
        definitions.put(label, guardDefinition);
    }
    public static GuardDefinition getDefinition(String label){
        return definitions.get(label);
    }

    public Guard(String label, TypedVariable[] parameters) throws TypeCreationException{
        if(parameters == null){
            throw new TypeCreationException("Guard type initialised with null.");
        }
        // If same, last one wins
//        if(definitions.containsKey(label)){
//            throw new TypeCreationException("Guard with name " + label + " already exists.");
//        }
        this.label = label;
        this.parameters = parameters;
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
    public Set getAllValues() throws InfiniteValueTypeException {
        throw new InfiniteValueTypeException("Guard does not have a finite set of values.");
    }
    @Override
    public String name() {
        return label;
    }

    //TODO deal with below function
    @Override
    public Parser valueParser() throws Exception {
        throw new Exception("Use Guard.valueParser(communicationContext) instead of Guard.valueParser()");
    }

    public Parser valueParser(TypingContext communicationContext) throws Exception {
        TypingContext context = new TypingContext();
        context.setAll(communicationContext);
        for(TypedVariable v : parameters){
            context.set(v.getName(), v.getType());
        }

        Parser parser = Condition.parser(context);

        return parser;
    }
}
