package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Guard;
import recipe.lang.utils.exceptions.TypeCreationException;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationDuplicateGuardParameterTest {
    @Test public void duplicateParameterNamesAreRejectedWhileParsing() throws Exception {
        for (String declaration : new String[]{"flag: boolean, flag: boolean", "flag: boolean, flag: integer"}) {
            String text = source("observation-forall-constant")
                .replace("agent Sender", "guard ambiguous(" + declaration + ") := TRUE;\nagent Sender");
            RuntimeException error = assertThrows(RuntimeException.class, () -> parse(text));
            assertTrue(error.toString(), error.toString().contains("Duplicate") && error.toString().contains("flag"));
        }
    }

    @Test public void directGuardConstructionRejectsDuplicateParameters() {
        TypedVariable flag = new TypedVariable(recipe.lang.types.Boolean.getType(), "flag");
        assertThrows(TypeCreationException.class, () -> new Guard("ambiguous", new TypedVariable[]{flag, flag}));
    }

    @Test public void distinctParameterNamesKeepTheirArgumentPositions() throws Exception {
        String text = source("observation-forall-constant")
            .replace("agent Sender", "guard second(first: boolean, last: boolean) := last;\nagent Sender")
            .replace("send: <TRUE>", "send: <second(FALSE, TRUE)>");
        assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
        assertCtl(ToNuXmv.transform(parse(text.replace("second(FALSE, TRUE)", "second(TRUE, FALSE)"))),
            "EX receiver-received", false);
    }
}
