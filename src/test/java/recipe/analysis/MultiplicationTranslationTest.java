package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class MultiplicationTranslationTest {
    @Test
    public void integerMultiplicationProducesValidSmv() throws Exception {
        assertValid(ToNuXmv.transform(parse(source("arithmetic-multiplication"))));
    }

    @Test
    public void multiplicationAndBroadcastCanCoexist() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..8")
            .replace("send: <TRUE>", "send: <count == 2>")
            .replace("c!", "*!").replace("c?", "*?");
        assertCtl(ToNuXmv.transform(parse(model)), "EX (sender-count = 4 & receiver-received)", true);
    }

    @Test
    public void parenthesizedOperandsRemainArithmetic() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..8")
            .replace("send: <TRUE>", "send: <count == 2>")
            .replace("count * 2", "(count + 1) * 2");
        assertCtl(ToNuXmv.transform(parse(model)), "EX sender-count = 6", true);
    }

    @Test
    public void multiplicationOperandsAreNotMistakenForBroadcastAtoms() throws Exception {
        String[] expressions = {"count * count", "count * (count + 1)", "count * (0 - count) + 8"};
        int[] results = {4, 6, 4};
        for (int i = 0; i < expressions.length; i++) {
            String model = source("arithmetic-multiplication")
                .replace("count: integer", "count: 0..8, ch: channel")
                .replace("init: !sent & count == 2", "init: !sent & count == 2 & ch == *")
                .replace("send: <TRUE>", "send: <count == 2>")
                .replace("c!", "*!").replace("c?", "*?")
                .replace("count * 2", expressions[i]);
            assertCtl(ToNuXmv.transform(parse(model)),
                "EX (sender-count = " + results[i] + " & sender-ch = broadcast & receiver-received)", true);
        }
    }
}
