package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class TemporalUnaryScopeTest {
    @Test
    public void unaryOperatorsDoNotConsumeFollowingBooleanConnectives() throws Exception {
        for (String formula : new String[] {
            "!sender-sent | first-received", "!sender-sent & first-received",
            "X sender-sent | first-received", "G sender-sent | first-received",
            "F sender-sent & first-received", "!(sender-sent | first-received)",
            "X (sender-sent | first-received)", "!!sender-sent | first-received"
        }) {
            String output = ToNuXmv.transform(parse(source("supplier-locality") + "\nSPEC " + formula + ";\n"));
            assertValid(output);
            String actual = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
            assertLtl(output, "G ((" + actual + ") <-> (" + formula + "))", true);
        }
    }

    @Test
    public void arithmeticComparisonsRemainAtoms() throws Exception {
        String output = ToNuXmv.transform(parse(source("supplier-locality")
            + "\nSPEC !sender-sent | (sender-automaton-state + 1) > 0;\n"));
        assertValid(output);
        String actual = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
        assertLtl(output, actual, true);
    }

    @Test
    public void guardAtomsStillAcceptCompoundArguments() throws Exception {
        recipe.lang.utils.TypingContext context = new recipe.lang.utils.TypingContext();
        recipe.lang.definitions.GuardDefinition definition = recipe.lang.definitions.GuardDefinition
            .parser(context).end().parse("guard both(a: boolean, b: boolean) := a & b;").get();
        recipe.lang.types.Guard.setDefinition("both", definition);
        context.set("both", definition.getType());
        for (String name : new String[] {"x", "y", "z"}) context.set(name, recipe.lang.types.Boolean.getType());
        org.petitparser.context.Result result = recipe.lang.expressions.predicate.Condition.atomicParser(context)
            .end().parse("both(x | y, z)");
        org.junit.Assert.assertTrue(result.toString(), result.isSuccess());
        recipe.lang.expressions.predicate.GuardReference reference = result.get();
        assertLtl("MODULE main\nVAR x : boolean; y : boolean; z : boolean;\n",
            "G ((" + reference.unpack() + ") <-> ((x | y) & z))", true);
    }
}
