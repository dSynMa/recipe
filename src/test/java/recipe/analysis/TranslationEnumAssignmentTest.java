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
            .replace("SPEC G (<forall(TRUE)>true);", "");
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

    @Test public void booleanLookingChannelPayloadsAreTypedByTheirTarget() throws Exception {
        for (String channel : new String[]{"false", "true"}) for (boolean supply : new boolean[]{false, true}) {
            String text = model("channel", channel).replace("channels: c", "channels: c, " + channel);
            assertCtl(ToNuXmv.transform(parse(mode(text, supply))), "EX receiver-received", true);
        }
    }

    @Test public void booleanLookingEnumPayloadsAreTypedByTheirTarget() throws Exception {
        for (String literal : new String[]{"False", "True"}) {
            String text = model("shade", literal).replace("channels: c", "channels: c\nenum shade {False, True}");
            assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
        }
    }

    @Test public void enumLocalUpdatesPreserveBooleanLookingLiteral() throws Exception {
        String text = model("channel", "c").replace("channels: c", "channels: c, false")
            .replace("local: sent: boolean", "local: sent: boolean, chosen: channel")
            .replace("init: !sent", "init: !sent & chosen == c")
            .replace("sent := TRUE", "sent := TRUE, chosen := false");
        assertCtl(ToNuXmv.transform(parse(text)), "EX (sender-chosen = false)", true);
    }

    @Test public void mixedAssignmentsUseEachTargetsType() throws Exception {
        for (String updates : new String[]{"sent := TRUE, chosen := false, count := count + 1",
                "chosen := false, count := count + 1, sent := TRUE",
                "count := count + 1, sent := TRUE, chosen := false"}) {
            String text = model("channel", "c").replace("channels: c", "channels: c, false")
                .replace("local: sent: boolean", "local: sent: boolean, chosen: channel, count: 0..1")
                .replace("init: !sent", "init: !sent & chosen == c & count == 0")
                .replace("sent := TRUE", updates);
            assertCtl(ToNuXmv.transform(parse(text)), "EX (sender-chosen = false & sender-count = 1 & sender-sent)", true);
        }
    }

    @Test public void malformedEnumExpressionsCannotSucceedByPrefix() throws Exception {
        for (String payload : new String[]{"c + 1", "c == c", "false & TRUE", "falseSuffix"}) {
            String text = model("channel", "c").replace("channels: c", "channels: c, false")
                .replace("value := c", "value := " + payload);
            try {
                assertTrue(payload, recipe.lang.System.parser().end().parse(text).isFailure());
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("Invalid assignment"));
            }
        }
    }

    @Test public void inScopeVariablesTakePrecedenceOverEnumLiterals() throws Exception {
        String text = model("channel", "c").replace("channels: c", "channels: c, false")
            .replace("local: sent: boolean", "local: sent: boolean, chosen: channel, false: channel")
            .replace("init: !sent", "init: !sent & chosen == false & false == c")
            .replace("sent := TRUE", "sent := TRUE, chosen := false");
        // The variable named false contains c, so copying it must not assign the literal false.
        assertCtl(ToNuXmv.transform(parse(text)), "EX (sender-chosen = c)", true);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse(text
            .replace("false: channel", "false: boolean")
            .replace("chosen == false", "chosen == c")
            .replace(" & false == c", "")));
        assertTrue(error.getMessage(), error.getMessage().contains("Invalid assignment"));
    }
}
