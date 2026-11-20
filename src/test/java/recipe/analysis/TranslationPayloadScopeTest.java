package recipe.analysis;

import org.junit.Test;
import org.petitparser.context.Result;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationPayloadScopeTest {
    private String model(boolean supply, String payload) throws Exception {
        String text = source("observation-forall-constant").replace("value := TRUE", "value := " + payload);
        return supply ? text.replace("c! (@ready)", "SUPPLY@(@ready)").replace("c?", "GET@TRUE") : text;
    }

    @Test public void payloadCannotReadReceiverCommonVariables() throws Exception {
        for (boolean supply : new boolean[]{false, true}) {
            Result result = recipe.lang.System.parser().end().parse(model(supply, "@ready"));
            assertTrue(result.toString(), result.isFailure());
        }
    }

    @Test public void guardMacroCannotHideOutOfScopePayloadVariables() throws Exception {
        for (boolean supply : new boolean[]{false, true}) {
            String text = model(supply, "targetReady()")
                .replace("agent Sender", "guard targetReady() := @ready;\nagent Sender");
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse(text));
            assertTrue(error.getMessage(), error.getMessage().contains("not available in assignment"));
        }
    }

    @Test(timeout=30000) public void senderLocalsAndExplicitGuardArgumentsRemainValid() throws Exception {
        for (boolean supply : new boolean[]{false, true}) for (String payload : new String[]{"!sent", "identity(!sent)"}) {
            String text = model(supply, payload)
                .replace("agent Sender", "guard identity(x: boolean) := x;\nagent Sender");
            assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
        }
    }
}
