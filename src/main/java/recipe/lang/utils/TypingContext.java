package recipe.lang.utils;

import recipe.lang.expressions.Expression;

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

    public TypingContext(Map<String, Expression> varType){
        this.varType = varType;
        this.typeVars = new HashMap<>();
        for(String name : varType.keySet()){
            Class<? extends Expression> type = varType.get(name).getClass();
            if(!typeVars.containsKey(type)){
                typeVars.put(type, new HashSet<>());
            }

            typeVars.get(type).add(name);
        }
    }

    public TypingContext(Map<String, Expression> varType, Map<Class<? extends Expression>, Set<String>> typeVars){
        this.varType = varType;
        this.typeVars = typeVars;
    }

    public void set(String name, Expression typedExpression){
        varType.put(name, typedExpression);

        if(!typeVars.containsKey(typedExpression.getClass())){
            typeVars.put(typedExpression.getClass(), new HashSet<>());
        }

        typeVars.get(typedExpression.getClass()).add(name);
    }

    public void setAll(TypingContext context){
        for(Map.Entry<String, Expression> en : varType.entrySet()){
            set(en.getKey(), en.getValue());
        }
    }

    public Expression get(String name){
        return varType.get(name);
    }

    public Set<String> get(Class<? extends Expression> type){
        if(typeVars.containsKey(type))
            return typeVars.get(type);
        else return new HashSet<>();
    }

    public TypingContext getSubContext(Class<? extends Expression> type){
        Set<String> vals = get(type);
        TypingContext context = new TypingContext();
        for(String val : vals){
            context.set(val, varType.get(val));
        }

        return context;
    }

    public static TypingContext union(TypingContext context1, TypingContext context2){
        TypingContext newContext = new TypingContext();

        newContext.setAll(context1);
        newContext.setAll(context2);

        return newContext;
    }
}
