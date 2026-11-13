package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SymbolicDivisionSemanticsTest {
    private String fixedCounter(String value, String expression) throws Exception {
        return source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..3")
            .replace("count == 2", "count == " + value)
            .replace("count * 2", "count")
            .replace("ready <- !received", "ready <- TRUE")
            + "\nSPEC " + expression + ";\n";
    }

    @Test
    public void boundedOperandsCannotSilentlyUseTruncatedBddDivision() throws Exception {
        String output = ToNuXmv.transform(parse(fixedCounter("1", "(sender-count / 2) * 2 == sender-count")));
        String formula = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
        // Previously BDD checking succeeded with the wrong answer (false). Real division
        // must either be checked correctly or require the real-capable SMT backend.
        assertThrows(AssertionError.class, () -> assertLtl(output, formula, false));
        assertSmtInvariant(output, formula);
    }

    @Test
    public void divisionBySymbolicBoundedDenominatorKeepsItsFraction() throws Exception {
        String output = ToNuXmv.transform(parse(fixedCounter("3", "(1 / sender-count) * sender-count == 1")));
        String formula = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
        assertThrows(AssertionError.class, () -> assertLtl(output, formula, false));
        assertSmtInvariant(output, formula);
    }
}
