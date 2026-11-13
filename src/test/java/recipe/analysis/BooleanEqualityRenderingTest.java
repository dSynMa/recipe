package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;
import recipe.lang.utils.TypingContext;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class BooleanEqualityRenderingTest {
    @Test
    public void bothEqualityOperatorsPreserveCompoundOperandsForAllValuations() throws Exception {
        TypingContext context = new TypingContext();
        for (String name : new String[] {"a", "b", "c"}) context.set(name, Boolean.getType());
        for (String formula : new String[] {
            "a == (b & c)", "(a | b) == c", "a != (b | c)", "(a & b) != c",
            "a == (b == c)", "a != (b != c)",
            "a & (b | c)", "(a | b) & c", "a | (b & c)", "(a & b) | c",
            "(a -> b) -> c", "a -> (b -> c)", "!(a | b)", "!a"
        }) {
            org.petitparser.context.Result result = Condition.parser(context).end().parse(formula);
            assertTrue(result.toString(), result.isSuccess());
            Condition condition = result.get();
            String output = "MODULE main\nVAR a : boolean; b : boolean; c : boolean;\n";
            for (String rendered : new String[] {condition.toString(), condition.toNuXmv()}) {
                assertLtl(output, "G ((" + rendered + ") <-> (" + formula.replace("==", "=") + "))", true);
            }
        }
    }

    @Test
    public void compoundRightOperandRemainsGroupedInTranslatedSpecification() throws Exception {
        String output = ToNuXmv.transform(parse(source("supplier-locality")
            + "\nSPEC sender-sent == (first-received & second-received);\n"));
        assertValid(output);
        assertCtl(output, output.substring(output.indexOf("LTLSPEC ") + 8).trim(), true);
    }
}
