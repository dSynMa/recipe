package recipe.analysis;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;
import static recipe.analysis.Smv.*;
import static recipe.analysis.TranslationTestModels.assertLtl;
import static recipe.analysis.TranslationTestModels.assertValid;

public class SmvTest {
    @Test
    public void compoundClausesPreserveBooleanGrouping() throws Exception {
        Smv.Module module = new Smv.Module();
        for (String name : List.of("a", "b", "c")) module.variable(name, "boolean");
        module.define("grouped", and(or(atom("a"), atom("b")), not(atom("c"))));
        module.define("equality", eq(or(atom("a"), atom("b")), atom("c")));
        module.define("guarded", and(atom("a"), or(atom("b"), atom("c"))));
        String output = module.render();
        assertLtl(output, "G (equality <-> ((a | b) = c))", true);
        assertLtl(output, "G (guarded <-> (a & (b | c)))", true);
        assertLtl(output, "G (grouped <-> ((a | b) & !c))", true);
        // This valuation distinguishes a grouped disjunction from a | (b & !c).
        assertLtl(output, "G ((a & c) -> !grouped)", true);
    }

    @Test
    public void expressionsSnapshotMutableClauseLists() {
        List<Smv.Expr> clauses = new ArrayList<>(List.of(atom("a"), atom("b")));
        Smv.Expr frozen = and(clauses);
        clauses.add(FALSE);
        assertEquals("(a) & (b)", render(frozen));
        assertEquals("TRUE", render(and(List.of())));
        assertEquals("FALSE", render(or(List.of())));
    }

    @Test
    public void moduleRendersNextStateAndSpecificationsWithoutEmptySections() throws Exception {
        Smv.Module module = new Smv.Module();
        module.variable("flag", "boolean");
        module.initially(eq(atom("flag"), FALSE));
        module.transition(eq(next(atom("flag")), not(atom("flag"))));
        module.specification("G F flag");
        String output = module.render();
        assertFalse(output.contains("DEFINE"));
        assertFalse(output.contains("CONSTANTS"));
        assertTrue(output.contains("LTLSPEC G F flag"));
        assertValid(output);
        assertLtl(output, "G (flag <-> X !flag)", true);
    }
}
