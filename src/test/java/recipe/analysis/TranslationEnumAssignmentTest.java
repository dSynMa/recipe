package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationEnumAssignmentTest {
    private String model(String domain, String payload) throws Exception {
        return source("observation-forall-constant")
            .replace("value: boolean", "value: " + domain)
            .replace("value := TRUE", "value := " + payload)
            .replace("receive: <value>", "receive: <value == " + payload + ">")
            .replace("received := value", "received := TRUE")
            .replace("SPEC G (<forall(TRUE)>trueValue);", "");
    }

    private String mode(String model, boolean supply) {
        return supply ? model.replace("c! (@ready)", "SUPPLY@(@ready)").replace("c?", "GET@TRUE") : model;
    }

    @Test public void sharedEnumLiteralsAreTypedByTheirTarget() throws Exception {
        for (String domain : new String[]{"channel", "shade"}) {
            String text = model(domain, "c").replace("channels: c", "channels: c\nenum shade {c, dark}");
            assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
        }
    }

    @Test public void prefixedBooleanChannelPayloadsAreTypedByTheirTarget() throws Exception {
        for (String channel : new String[]{"falseValue", "trueValue"}) for (boolean supply : new boolean[]{false, true}) {
            String text = model("channel", channel).replace("channels: c", "channels: c, " + channel);
            assertCtl(ToNuXmv.transform(parse(mode(text, supply))), "EX receiver-received", true);
        }
    }

    @Test public void prefixedBooleanEnumPayloadsAreTypedByTheirTarget() throws Exception {
        for (String literal : new String[]{"FalseValue", "TrueValue"}) {
            String text = model("shade", literal).replace("channels: c", "channels: c\nenum shade {FalseValue, TrueValue}");
            assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
        }
    }

    @Test public void enumLocalUpdatesPreservePrefixedBooleanLiteral() throws Exception {
        String text = model("channel", "c").replace("channels: c", "channels: c, falseValue")
            .replace("local: sent: boolean", "local: sent: boolean, chosen: channel")
            .replace("init: !sent", "init: !sent & chosen == c")
            .replace("sent := TRUE", "sent := TRUE, chosen := falseValue");
        assertCtl(ToNuXmv.transform(parse(text)), "EX (sender-chosen = falseValue)", true);
    }

    @Test public void mixedAssignmentsUseEachTargetsType() throws Exception {
        for (String updates : new String[]{"sent := TRUE, chosen := falseValue, count := count + 1",
                "chosen := falseValue, count := count + 1, sent := TRUE",
                "count := count + 1, sent := TRUE, chosen := falseValue"}) {
            String text = model("channel", "c").replace("channels: c", "channels: c, falseValue")
                .replace("local: sent: boolean", "local: sent: boolean, chosen: channel, count: 0..1")
                .replace("init: !sent", "init: !sent & chosen == c & count == 0")
                .replace("sent := TRUE", updates);
            assertCtl(ToNuXmv.transform(parse(text)), "EX (sender-chosen = falseValue & sender-count = 1 & sender-sent)", true);
        }
    }

    @Test public void malformedEnumExpressionsCannotSucceedByPrefix() throws Exception {
        for (String payload : new String[]{"c + 1", "c == c", "falseValue & TRUE", "falseSuffix"}) {
            String text = model("channel", "c").replace("channels: c", "channels: c, falseValue")
                .replace("value := c", "value := " + payload);
            try {
                assertTrue(payload, recipe.lang.System.parser().end().parse(text).isFailure());
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("Invalid assignment"));
            }
        }
    }

    @Test public void inScopeVariablesTakePrecedenceOverEnumLiterals() throws Exception {
        String text = model("channel", "c").replace("channels: c", "channels: c, falseValue")
            .replace("local: sent: boolean", "local: sent: boolean, chosen: channel, falseValue: channel")
            .replace("init: !sent", "init: !sent & chosen == falseValue & falseValue == c")
            .replace("sent := TRUE", "sent := TRUE, chosen := falseValue");
        // The variable named falseValue contains c, so copying it must not assign the literal falseValue.
        assertCtl(ToNuXmv.transform(parse(text)), "EX (sender-chosen = c)", true);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse(text
            .replace("falseValue: channel", "falseValue: boolean")
            .replace("chosen == falseValue", "chosen == c")
            .replace(" & falseValue == c", "")));
        assertTrue(error.getMessage(), error.getMessage().contains("Invalid assignment"));
    }
}
