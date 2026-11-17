package recipe.analysis;

import org.junit.Test;
import java.util.List;
import java.util.Map;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.Predicate;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.Not;
import recipe.lang.expressions.predicate.Or;
import recipe.lang.store.ConcreteStore;
import recipe.lang.types.Boolean;
import recipe.lang.types.Type;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ImplicitObservationQuantifierTest {
    @Test
    public void bareVariablesAndNegationsMatchExplicitQuantifiers() throws Exception {
        for (String selection : new String[] {"@ready", "!@ready", "TRUE", "FALSE"}) {
            for (String observation : new String[] {"ready", "!ready"}) {
                boolean expected = observation.equals("ready")
                    ? selection.equals("@ready") || selection.equals("FALSE")
                    : selection.equals("!@ready") || selection.equals("TRUE");
                String explicit = observation.equals("ready") ? "forall(ready)" : "exists(!ready)";
                for (String expression : new String[] {observation, explicit}) {
                    String model = source("observation-forall-constant")
                        .replace("(@ready)", "(" + selection + ")")
                        .replace("c!", "*!").replace("c?", "*?")
                        .replace("forall(TRUE)", expression);
                    String output = ToNuXmv.transform(parse(model));
                    assertValid(output);
                    assertCtl(output, "EX obs0", expected);
                }
            }
        }
    }

    @Test
    public void explicitQuantifierScopeIsNotChangedByImplicitLiterals() throws Exception {
        String model = source("observation-forall-constant").replace("(@ready)", "(TRUE)");
        for (String[] example : new String[][] {{"exists(ready)", "true"}, {"forall(ready)", "false"},
                {"ready | !ready", "true"}, {"ready & !ready", "false"}, {"!!ready", "false"}}) {
            String output = ToNuXmv.transform(parse(model.replace("forall(TRUE)", example[0])));
            assertCtl(output, "EX obs0", java.lang.Boolean.parseBoolean(example[1]));
        }
    }

    /** ReCiPe section 7 observation-descriptor semantics, https://arxiv.org/html/2104.10998v3#S7. */
    @Test
    public void allBooleanSelectionSetsMatchPaperSemantics() throws Exception {
        TypedVariable<Boolean> p = new TypedVariable<>(Boolean.getType(), "p");
        TypedVariable<Boolean> q = new TypedVariable<>(Boolean.getType(), "q");
        TypedVariable<Boolean> sendP = new TypedVariable<>(Boolean.getType(), "@p");
        TypedVariable<Boolean> sendQ = new TypedVariable<>(Boolean.getType(), "@q");
        List<Expression<Boolean>> observations = List.of(
            p, new Not(p), q, new Not(q),
            new Or(p, q), new And(p, q), new Not(new Or(p, q)), new Not(new Not(p)),
            new Predicate("exists", p), new Predicate("exists", new Not(p)),
            new Predicate("forall", new Or(p, q)), new Predicate("forall", new Not(p)));

        // Four valuations give sixteen possible sender predicates, including the empty set.
        for (int selected = 0; selected < 16; selected++) {
            Expression<Boolean> selection = Condition.getFalse();
            boolean allP = true, allQ = true, anyP = false;
            boolean allEither = true, allNotP = true;
            for (int valuation = 0; valuation < 4; valuation++) {
                if ((selected & (1 << valuation)) == 0) continue;
                boolean pValue = (valuation & 1) != 0;
                boolean qValue = (valuation & 2) != 0;
                selection = new Or(selection, new And(
                    pValue ? sendP : new Not(sendP), qValue ? sendQ : new Not(sendQ)));
                // Independent set-based oracle; do not call the translator to get expectations.
                allP &= pValue;
                allQ &= qValue;
                anyP |= pValue;
                allEither &= pValue || qValue;
                allNotP &= !pValue;
            }
            boolean[] expected = {allP, !allP, allQ, !allQ,
                allP || allQ, allP && allQ, !(allP || allQ), allP,
                anyP, !allP, allEither, allNotP};
            for (String prefix : new String[] {"", "@"}) {
                Map<String, Type> cvs = Map.of(prefix + "p", Boolean.getType(),
                    prefix + "q", Boolean.getType());
                for (int i = 0; i < observations.size(); i++) {
                    Expression<Boolean> actual = ToNuXmv.handleCVsInObservation(
                        cvs, observations.get(i), selection);
                    assertEquals("selected=" + selected + ", observation=" + observations.get(i)
                            + ", prefix=" + prefix,
                        expected[i], actual.valueIn(new ConcreteStore(Map.of())).getValue());
                }
            }
        }
    }

    @Test
    public void ordinaryBooleanVariablesAreNotImplicitlyQuantified() throws Exception {
        TypedVariable<Boolean> payload = new TypedVariable<>(Boolean.getType(), "payload");
        assertSame(payload, ToNuXmv.handleCVsInObservation(
            Map.of("ready", Boolean.getType()), payload, Condition.getFalse()));
    }

}
