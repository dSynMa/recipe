package recipe.lang.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LazyTypingContext {
    public LazyTypingContext(List<AtomicReference<TypingContext>> contexts) {
        this.contexts = contexts;
    }

    public LazyTypingContext(AtomicReference<TypingContext>... contexts) {
        this.contexts = new ArrayList<AtomicReference<TypingContext>>();
        for(AtomicReference<TypingContext> c : contexts) {
            this.contexts.add(c);
        }
    }

    public LazyTypingContext(AtomicReference<TypingContext> context) {
        this.contexts = new ArrayList<AtomicReference<TypingContext>>();
        this.contexts.add(context);
    }

    List<AtomicReference<TypingContext>> contexts;

    public TypingContext resolve(){
        TypingContext context = new TypingContext();

        for(AtomicReference<TypingContext> tc : contexts){
            context.setAll(tc.get());
        }

        return context;
    }
}
