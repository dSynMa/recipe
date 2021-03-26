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
}
