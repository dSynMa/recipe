package recipe.lang.types;

import org.petitparser.parser.Parser;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.TypeCreationException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.utils.Parsing;

import java.util.*;

public class Enum extends Type {
    private List<String> values;

    //TODO this should be in TypingContext
    private static Map<String, Enum> existing = new HashMap<>();
    public static void clear(){
        existing.clear();
    }

    private String label;

    public Enum(String label, List<String> values) throws TypeCreationException {
        if(values == null){
            throw new TypeCreationException("Enum type initialised with null.");
        }
        if(existing.containsKey(label)){
            throw new TypeCreationException("Enum with name " + label + " already exists.");
        }
        this.label = label;
        this.values = values;
        existing.put(label, this);
    }

    public List<String> getValues() {
        return values;
    }

    public static Enum getEnum(String name) throws Exception {
        if(existing.containsKey(name)) return existing.get(name);
        else throw new Exception("No such enum: " + name);
    }

    public static Set<String> getEnumLabels() {
        return existing.keySet();
    }

    public org.petitparser.parser.Parser valueParser(){
        return Parsing.disjunctiveWordParser(values, (String name) -> {
            try {
                return new TypedValue(this, name);
            } catch (MismatchingTypeException e) {
                e.printStackTrace();
            }
            return null;
        });
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Enum anEnum = (Enum) o;
        return label == ((Enum) o).label && new HashSet<>(values).equals(new HashSet<>(anEnum.values));
    }

    @Override
    public String interpret(String value) throws MismatchingTypeException {
        if(values.contains(value)){
            return value;
        } else{
            throw new MismatchingTypeException(value + " is not of type " + name());
        }
    }
    public Set<TypedValue> getAllValues() throws MismatchingTypeException {
        Set<TypedValue> typedValues = new HashSet<>();
        for(String v : values){
            typedValues.add(new TypedValue(this, v));
        }
        return typedValues;
    }
    @Override
    public String name(){
        return label;
    }

    public static Parser generalValueParser() throws Exception {
        Parser parser = null;
        for(String label : Enum.getEnumLabels()){
            recipe.lang.types.Enum enumm = Enum.getEnum(label);
            if (parser == null) {
                parser = enumm.valueParser();
            } else{
                parser = parser.or(enumm.valueParser());
            }
        }

        return parser;
    }

    public static boolean exists(String name){
        return existing.containsKey(name);
    }
}
