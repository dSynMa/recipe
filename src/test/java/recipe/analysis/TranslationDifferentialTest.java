package recipe.analysis;

import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

/** Deterministic differential audit of combinations beyond the individual bug reproductions. */
public class TranslationDifferentialTest {
    private String[] formula(Random random, int depth) {
        String[] atoms = {"TRUE", "FALSE", "sender-sent", "first-received", "second-received"};
        if (depth == 0) {
            String atom = atoms[random.nextInt(atoms.length)];
            return new String[]{atom, atom};
        }
        String[] left = formula(random, depth - 1);
        if (random.nextBoolean()) {
            String op = new String[]{"!", "G", "F", "X"}[random.nextInt(4)];
            return new String[]{op + " (" + left[0] + ")", op + " (" + left[1] + ")"};
        }
        String[] right = formula(random, depth - 1);
        String op = new String[]{"&", "|", "->", "<->", "U", "R", "W"}[random.nextInt(7)];
        String input = "(" + left[0] + ") " + op + " (" + right[0] + ")";
        String expected = op.equals("W") ? "((" + left[1] + ") U (" + right[1] + ")) | G (" + left[1] + ")"
            : "(" + left[1] + ") " + (op.equals("R") ? "V" : op) + " (" + right[1] + ")";
        return new String[]{input, expected};
    }

    @Test
    public void temporalOperatorCombinationsAgreeWithNativeNuXmv() throws Exception {
        Random random = new Random(4321);
        for (int i = 0; i < 40; i++) {
            String[] formula = formula(random, 3);
            String output = ToNuXmv.transform(parse(source("supplier-locality") + "\nSPEC " + formula[0] + ";\n"));
            String translated = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
            assertLtl(output, "G ((" + translated + ") <-> (" + formula[1] + "))", true);
        }
    }

}
