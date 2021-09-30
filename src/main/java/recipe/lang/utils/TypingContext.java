package recipe.lang.utils;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.EpsilonParser;
import org.petitparser.parser.primitive.FailureParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.Config;
import recipe.lang.definitions.GuardDefinition;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.*;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Integer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TypingContext {
    Set<Type> types;
    Map<String, Type> varType;
    Map<Type, Set<String>> typeVars;

    public Map<String, Type> getVarType() {
        return varType;
    }

    public Map<Type, Set<String>> getTypeVars() {
        return typeVars;
    }

    public TypingContext(){
        varType = new HashMap<>();
        typeVars = new HashMap<>();
        types = new HashSet<>();
    }

    public TypingContext(Map<String, Type> varType){
        types = new HashSet<>();
        this.varType = varType;
        typeVars = new HashMap<>();
        for(String name : varType.keySet()){
            Type type = varType.get(name);
            if(!typeVars.containsKey(type)){
                typeVars.put(type, new HashSet<>());
                types.add(type);
            }

            typeVars.get(type).add(name);
        }
    }

    public TypingContext(Set<Type> types, Map<String, Type> varType, Map<Type, Set<String>> typeVars){
        this.types = types;
        this.varType = varType;
        this.typeVars = typeVars;
    }

    public void remove(String name){
        Type type = varType.get(name);
        typeVars.get(type).remove(name);
        varType.remove(name);
    }

    public void set(String name, Type type){
        varType.put(name, type);
        if(type == null){
            System.out.println("");
        }

        if(!typeVars.containsKey(type)){
            typeVars.put(type, new HashSet<>());
        }

        types.add(type);
        typeVars.get(type).add(name);
    }

    public void setAll(TypingContext context){
        for(Map.Entry<String, Type> en : context.varType.entrySet()){
            types.add(en.getValue());
            set(en.getKey(), en.getValue());
        }
    }

    public Type get(String name){
        return varType.get(name);
    }

    public Set<String> get(Type type){
        HashSet vars = new HashSet<>();
        for(Map.Entry<Type, Set<String>> entries : typeVars.entrySet()){
            if(type.getClass().isAssignableFrom(entries.getKey().getClass())){
                vars.addAll(entries.getValue());
            }
        }
        return vars;
    }

    public TypingContext getSubContext(Type type){
        Set<String> vals = get(type);
        TypingContext context = new TypingContext();
        for(String val : vals){
            context.set(val, varType.get(val));
        }

        return context;
    }

    public TypingContext getComplementSubContext(Type type){
        TypingContext context = new TypingContext();
        for(Map.Entry<Type, Set<String>> entries : typeVars.entrySet()){
            if(!type.getClass().isAssignableFrom(entries.getKey().getClass())) {
                for(String val : entries.getValue()){
                    context.set(val, varType.get(val));
                }
            }
        }

        return context;
    }

    public static TypingContext union(TypingContext context1, TypingContext context2){
        TypingContext newContext = new TypingContext();

        newContext.setAll(context1);
        newContext.setAll(context2);

        return newContext;
    }

    public org.petitparser.parser.Parser guardNameParser(){
        Parser parser = null;

        Set<String> labels = Guard.definitonLabels();
        for(String v : labels){
            if(parser == null){
                parser = StringParser.of(v);
            } else{
                parser = parser.or(StringParser.of(v));
            }
        }

        if(parser == null){
            parser = FailureParser.withMessage("Expected to parse guard reference, but no guard definitions.");
        }

        return parser;
    }

    public org.petitparser.parser.Parser variableParser(){
        return Parsing.disjunctiveWordParser(varType.keySet(), (String parsed) -> {
            return new TypedVariable(varType.get(parsed), parsed);
        });
    }

    public org.petitparser.parser.Parser valueParser() throws Exception {
        if(this.varType.values().size() == 0) return new EpsilonParser();
        Parser parser = null;
        for(Type type : this.varType.values()){
            if(parser == null){
                parser = type.valueParser();
            }
            else{
                parser = parser.or(type.valueParser());
            }
        }

        return parser;
    }
}
