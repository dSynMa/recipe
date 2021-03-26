package recipe.lang.utils;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TypingContext {
    Map<String, Expression> varType;
    Map<Class<? extends Expression>, Set<String>> typeVars;

    public Map<String, Expression> getVarType() {
        return varType;
    }

    public Map<Class<? extends Expression>, Set<String>> getTypeVars() {
        return typeVars;
    }

    public TypingContext(){
        varType = new HashMap<>();
        typeVars = new HashMap<>();
    }

    public TypingContext(Map<String, Expression> varType, Map<Class<? extends Expression>, Set<String>> typeVars){
        this.varType = varType;
        this.typeVars = typeVars;
    }

    public void set(String name, TypedVariable typedVariable){
        varType.put(name, typedVariable);

        if(!typeVars.containsKey(typedVariable.getClass())){
            typeVars.put(typedVariable.getClass(), new HashSet<>());
        }

        typeVars.get(typedVariable.getClass()).add(name);
    }

    public Expression get(String name){
        return varType.get(name);
    }

    public Set<String> get(Class<? extends Expression> type){
        if(typeVars.containsKey(type))
            return typeVars.get(type);
        else return new HashSet<>();
    }

    public static TypingContext union(TypingContext context1, TypingContext context2){
        Map<String, Expression> varType1 = context1.getVarType();
        Map<String, Expression> varType2 = context2.getVarType();

        Map<Class<? extends Expression>, Set<String>> typeVars1 = context1.getTypeVars();
        Map<Class<? extends Expression>, Set<String>> typeVars2 = context2.getTypeVars();

        Map<String, Expression> varType = new HashMap<>();
        varType.putAll(varType1);
        varType.putAll(varType2);

        Map<Class<? extends Expression>, Set<String>> typeVars = new HashMap<>();
        typeVars.putAll(typeVars1);
        typeVars.putAll(typeVars2);

        return new TypingContext(varType, typeVars);
    }
}
