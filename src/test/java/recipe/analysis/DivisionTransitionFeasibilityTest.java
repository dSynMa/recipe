package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class DivisionTransitionFeasibilityTest {
    private void assertEnabled(String text, boolean enabled) throws Exception {
        var model = parse(text);
        String smv = ToNuXmv.transform(model);
        String progress = runNuXmv(smv, "go_msat\ncheck_ltlspec_ic3 -i -p \"" +
            (enabled ? "progress" : "!progress") + "\"\nquit\n");
        assertTrue(progress, progress.contains(" is true"));
        String transition = runNuXmv(smv,
            "go_msat\nmsat_check_ltlspec_bmc -k 3 -p \"G !sender-sent\"\nquit\n");
        assertTrue(transition, transition.contains(
            enabled ? " is false" : "no counterexample found with bound 3"));
        assertFalse(transition, transition.contains("Division by zero"));
        assertFalse(transition, transition.contains("There is a division by 0"));
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        assertEquals(!enabled, interpreter.isDeadlocked());
    }

    @Test(timeout = 30000)
    public void senderUpdatesRequireEveryNestedDivisorToBeNonzero() throws Exception {
        for (String type : new String[]{"integer", "real"}) {
            for (String update : new String[]{"count / (count - 2)", "1 / 0", "count / (1 / (count - 2))",
                    "1 + count / (count - 2)", "1 - count / (count - 2)", "2 * (count / (count - 2))"}) {
                assertEnabled(source("arithmetic-multiplication").replace("count: integer", "count: " + type)
                    .replace("count * 2", update), false);
            }
        }
        assertEnabled(source("arithmetic-multiplication").replace("count * 2", "count / (count - 1)"), true);
    }

    @Test(timeout = 30000)
    public void receiveSupplyAndGetUpdatesRejectZeroDivisors() throws Exception {
        String text = source("arithmetic-multiplication")
            .replace("local: received: boolean", "local: received: boolean, n: real")
            .replace("init: !received", "init: !received & n == 0")
            .replace("received := value", "received := value, n := 1 / n");
        assertEnabled(text, false);
        assertEnabled(text.replace("c! (@ready)", "SUPPLY@(TRUE)").replace("c?", "GET@TRUE"), false);
        assertEnabled(source("arithmetic-multiplication").replace("count * 2", "count / (count - 2)")
            .replace("c! (@ready)", "SUPPLY@(TRUE)").replace("c?", "GET@TRUE"), false);
        assertEnabled(text.replace("n == 0", "n == 1"), true);
    }

    @Test(timeout = 30000)
    public void payloadsAndBooleanExpressionsRequireDefinedDivision() throws Exception {
        for (String output : new String[]{"count / (count - 2) > 0", "!(count / (count - 2) == 0)"}) {
            assertEnabled(source("arithmetic-multiplication").replace("value := TRUE", "value := " + output), false);
            assertEnabled(source("arithmetic-multiplication").replace("send: <TRUE>", "send: <" + output + ">"), false);
        }
        assertEnabled(source("arithmetic-multiplication")
            .replace("message-structure: value: boolean", "message-structure: value: boolean, amount: real")
            .replace("value := TRUE", "value := TRUE, amount := 1 / (count - 2)"), false);
    }
    @Test(timeout = 30000)
    public void undefinedAlternativeDoesNotSuppressAValidSend() throws Exception {
        String text = source("arithmetic-multiplication").replace("count * 2", "count / (count - 2)")
            .replace("count := count / (count - 2)]", "count := count / (count - 2)]"
                + " + safe: <TRUE> c! (@ready)(value := TRUE)[sent := TRUE, count := count]");
        assertEnabled(text, true);
        Interpreter interpreter = new Interpreter(parse(text));
        interpreter.init("TRUE");
        interpreter.next(0);
        assertEquals("safe!", interpreter.getCurrentStep().toJSON().getJSONObject("state")
            .getJSONObject("sender").getString("**last_label**"));
    }

    @Test(timeout = 30000)
    public void guardsAndSelectionsRejectUndefinedDivision() throws Exception {
        String base = source("arithmetic-multiplication");
        assertEnabled(base.replace("agent Sender", "guard positive(a: real) := 1 / (a - 2) > 0;\nagent Sender")
            .replace("send: <TRUE>", "send: <positive(count)>"), false);
        assertEnabled(base.replace("c! (@ready)", "c! (count / (count - 2) > 0)"), false);
        assertEnabled(base.replace("c! (@ready)", "SUPPLY@(count / (count - 2) > 0)")
            .replace("c?", "GET@TRUE"), false);
    }

    @Test(timeout = 30000)
    public void receiverDivisorsAreCheckedAfterMessageSubstitution() throws Exception {
        String text = source("arithmetic-multiplication")
            .replace("message-structure: value: boolean", "message-structure: value: boolean, amount: real")
            .replace("value := TRUE", "value := TRUE, amount := 0")
            .replace("local: received: boolean", "local: received: boolean, n: real")
            .replace("init: !received", "init: !received & n == 1")
            .replace("received := value", "received := value, n := 1 / amount");
        assertEnabled(text, false);
        assertEnabled(text.replace("c! (@ready)", "SUPPLY@(TRUE)").replace("c?", "GET@TRUE"), false);
        assertEnabled(text.replace("amount := 0", "amount := 1"), true);
    }
}
