package recipe.lang.utils;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.EpsilonParser;
import org.petitparser.parser.primitive.FailureParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.*;
import recipe.lang.types.Enum;

import java.util.*;

public class TypingContext {
    Set<Type> types;
    Map<String, Type> varType;
    Map<Type, Set<String>> typeVars;
    Set<String> predicates;

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
        predicates = new HashSet<>();
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

    public TypingContext(List<TypedVariable> varss){
        types = new HashSet<>();
        this.varType = new HashMap<>();
        typeVars = new HashMap<>();
        for(TypedVariable v : varss){
            Type type = v.getType();
            String name = v.getName();
            if(!typeVars.containsKey(type)){
                typeVars.put(type, new HashSet<>());
                types.add(type);
            }

            typeVars.get(type).add(name);
            varType.put(name, type);
        }
    }

    public TypingContext(Set<Type> types, Map<String, Type> varType, Map<Type, Set<String>> typeVars){
        this.types = types;
        this.varType = varType;
        this.typeVars = typeVars;
    }

    public void clear(){
        varType = new HashMap<>();
        typeVars = new HashMap<>();
        types = new HashSet<>();
        predicates = new HashSet<>();
    }

    public void remove(String name){
        Type type = varType.get(name);
        typeVars.get(type).remove(name);
        varType.remove(name);
    }

    public void addPredicate(String name){
        predicates.add(name);
    }

    public Set<String> getPredicates(){
        return predicates;
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
            if(type.getClass().isAssignableFrom(entries.getKey().getClass())) {
                if (type.getClass().equals(Enum.class) && entries.getKey().getClass().equals(Enum.class)) {
                    if (((Enum) type).name().equals(((Enum) entries.getKey()).name())) {
                        vars.addAll(entries.getValue());
                    }
                } else {
                    vars.addAll(entries.getValue());
                }
            }

            if(type.getClass().equals(UnionType.class)){
                if(entries.getKey().getClass().equals(UnionType.class)){
                    if(!Collections.disjoint(((UnionType) type).getTypes(), ((UnionType) entries.getKey()).getTypes())){
                        vars.addAll(entries.getValue());
                    }
                } else if(((UnionType) type).getTypes().contains(entries.getKey())){
                    vars.addAll(entries.getValue());
                }
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

        Set<String> labels = Guard.definitionLabels();
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
