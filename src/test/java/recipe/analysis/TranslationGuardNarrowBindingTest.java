package recipe.analysis;

import org.junit.Test;
import recipe.lang.definitions.GuardDefinition;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.GuardReference;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.store.ConcreteStore;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Guard;
import recipe.lang.types.Type;
import recipe.lang.utils.TypingContext;
import java.util.Collections;
import static org.junit.Assert.*;

public class TranslationGuardNarrowBindingTest {
    @Test public void acceptedNarrowFreeVariableEvaluatesInCallerStore() throws Exception {
        assertFreeBindings(new BoundedInteger(0, 2), new BoundedInteger(0, 1));
    }

    @Test public void acceptedIntegerFreeVariableEvaluatesInRealTemplate() throws Exception {
        assertFreeBindings(recipe.lang.types.Real.getType(), recipe.lang.types.Integer.getType());
    }

    private void assertFreeBindings(Type expected, Type actual) throws Exception {
        TypedVariable templateVar = new TypedVariable(expected, "free");
        Guard guard = new Guard("narrowAudit", new TypedVariable[0]);
        GuardDefinition definition = new GuardDefinition(guard,
            new IsEqualTo(templateVar, new TypedValue(expected, "1")));
        Guard.setDefinition(guard.name(), definition);
        GuardReference narrow = parseCall(guard, actual, "narrowAudit()");
        GuardReference original = parseCall(guard, expected, "narrowAudit()");
        assertEvaluation(narrow, actual, "1", true);
        assertEvaluation(narrow, actual, "0", false);
        assertEvaluation(original, expected, "1", true);
        assertEvaluation(narrow, actual, "1", true);
        assertSame(definition, Guard.getDefinition(guard.name()));
    }

    @Test public void callerVariableMatchingFormalNameKeepsArgumentBinding() throws Exception {
        Type expected = new BoundedInteger(0, 2);
        Type actual = new BoundedInteger(0, 1);
        TypedVariable parameter = new TypedVariable(expected, "free");
        Guard guard = new Guard("formalAudit", new TypedVariable[]{parameter});
        Guard.setDefinition(guard.name(), new GuardDefinition(guard,
            new IsEqualTo(parameter, new TypedValue(expected, "1"))));
        assertEvaluation(parseCall(guard, actual, "formalAudit(1)"), actual, "0", true);
        assertEvaluation(parseCall(guard, actual, "formalAudit(free)"), actual, "1", true);
        assertEvaluation(parseCall(guard, actual, "formalAudit(free)"), actual, "0", false);
    }

    private GuardReference parseCall(Guard guard, Type actual, String call) throws Exception {
        TypingContext context = new TypingContext();
        context.set("free", actual);
        context.set(guard.name(), guard);
        return Condition.parser(context).end().parse(call).get();
    }

    private void assertEvaluation(GuardReference expression, Type actual, String value, boolean expected) throws Exception {
        ConcreteStore store = new ConcreteStore(Collections.singletonMap(
            new TypedVariable(actual, "free"), new TypedValue(actual, value)));
        assertEquals(new TypedValue(recipe.lang.types.Boolean.getType(), expected ? "TRUE" : "FALSE"), expression.valueIn(store));
        assertEquals(new TypedValue(recipe.lang.types.Boolean.getType(), expected ? "TRUE" : "FALSE"), expression.unpack().valueIn(store));
    }
}
