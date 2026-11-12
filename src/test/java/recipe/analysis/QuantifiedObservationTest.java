package recipe.analysis;

import java.util.Map;
import org.junit.Test;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.Predicate;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.Not;
import recipe.lang.types.Boolean;

import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class QuantifiedObservationTest {
    private final TypedVariable<Boolean> ready = new TypedVariable<>(Boolean.getType(), "@ready");

    @Test
    public void constantQuantifiersReturnTheirBooleanIdentities() throws Exception {
        assertQuantifier("forall", Condition.getTrue(), ready, true);
        assertQuantifier("exists", Condition.getFalse(), ready, false);
    }

    @Test
    public void emptySelectionHasVacuousUniversalAndFalseExistential() throws Exception {
        assertQuantifier("forall", ready, Condition.getFalse(), true);
        assertQuantifier("exists", ready, Condition.getFalse(), false);
    }

    @Test
    public void selectionIsAppliedBeforeQuantification() throws Exception {
        assertQuantifier("forall", ready, ready, true);
        assertQuantifier("exists", ready, new Not(ready), false);
    }

    @Test
    public void nonconstantQuantifiersStillEnumerateBothValues() throws Exception {
        assertQuantifier("forall", ready, Condition.getTrue(), false);
        assertQuantifier("exists", ready, Condition.getTrue(), true);
    }

    @Test
    public void universalTrueObservationCanBeObservedOnASend() throws Exception {
        String output = ToNuXmv.transform(parse(source("observation-forall-constant")));
        assertCtl(output, "EX obs0", true);
    }

    @Test
    public void existentialFalseObservationCanNeverBeObserved() throws Exception {
        String output = ToNuXmv.transform(parse(source("observation-exists-constant")));
        assertCtl(output, "AG !obs0", true);
    }

    private void assertQuantifier(String quantifier, Expression<Boolean> body,
            Expression<Boolean> guard, boolean expected) throws Exception {
        Expression<Boolean> result = ToNuXmv.handleCVsInObservation(
            Map.of("@ready", Boolean.getType(), "@other", Boolean.getType()),
            new Predicate(quantifier, body), guard);
        assertNotNull(quantifier, result);
        assertEquals(expected ? Condition.getTrue() : Condition.getFalse(), result.simplify());
    }
}
