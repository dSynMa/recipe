package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Type;
import recipe.lang.utils.TypingContext;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class TranslationTypingContextConstructorTest {
    private final Type bool = recipe.lang.types.Boolean.getType();

    @Test
    public void mapContextSupportsConditionsAndPredicates() throws Exception {
        assertConditionAndPredicate(new TypingContext(Collections.singletonMap("ready", bool)));
    }

    @Test
    public void listContextSupportsConditionsAndPredicates() throws Exception {
        assertConditionAndPredicate(new TypingContext(Collections.singletonList(new TypedVariable(bool, "ready"))));
    }

    @Test
    public void indexedContextSupportsConditionsAndPredicates() throws Exception {
        assertConditionAndPredicate(new TypingContext(Collections.singleton(bool),
            Collections.singletonMap("ready", bool),
            Collections.singletonMap(bool, Collections.singleton("ready"))));
    }

    @Test
    public void defaultContextSupportsConditionsAndPredicates() throws Exception {
        TypingContext context = new TypingContext();
        context.set("ready", bool);
        assertConditionAndPredicate(context);
    }

    private void assertConditionAndPredicate(TypingContext context) throws Exception {
        assertTrue(Condition.parser(context).end().parse("ready & TRUE").isSuccess());
        context.addPredicate("audit");
        assertTrue(Condition.parser(context).end().parse("audit ready").isSuccess());
    }
}
