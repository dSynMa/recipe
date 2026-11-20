package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsLessOrEqualThan;
import recipe.lang.types.Number;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

/** Semantic regression tests for comparison guards. */
public class ComparisonTranslationTest {
    private void assertSend(String guard, boolean enabled) throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..8")
            .replace("send: <TRUE>", "send: <!sent & (" + guard + ")>");
        String control = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..8")
            .replace("send: <TRUE>", "send: <!sent & " + (enabled ? "TRUE" : "FALSE") + ">");
        assertCtl(ToNuXmv.transform(parse(control)), "EX sender-sent", enabled);
        assertCtl(ToNuXmv.transform(parse(model)), "EX sender-sent", enabled);
    }

    @Test public void constantLessThanCanBeTrue() throws Exception { assertSend("1 < 2", true); }
    @Test public void constantGreaterThanCanBeFalse() throws Exception { assertSend("1 > 2", false); }
    @Test public void constantLessOrEqualCanBeTrue() throws Exception { assertSend("1 <= 2", true); }
    @Test public void constantGreaterOrEqualCanBeTrue() throws Exception { assertSend("2 >= 1", true); }
    @Test public void variableIsNotLessThanItself() throws Exception { assertSend("count < count", false); }
    @Test public void variableIsNotGreaterThanItself() throws Exception { assertSend("count > count", false); }

    @Test
    public void lessOrEqualEvaluatesNumericValues() throws Exception {
        TypedValue<Number> one = new TypedValue<>(recipe.lang.types.Integer.getType(), "1");
        TypedValue<Number> two = new TypedValue<>(recipe.lang.types.Integer.getType(), "2");
        assertEquals(Condition.getTrue(), new IsLessOrEqualThan(one, two).valueIn(null));
    }
}
