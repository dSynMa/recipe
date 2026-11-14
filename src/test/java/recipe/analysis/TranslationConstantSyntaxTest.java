package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationConstantSyntaxTest {
    @Test public void numericEnumConstantsFailBeforeEmittingInvalidSmv() throws Exception {
        for (String literal : new String[]{"0", "1", "123abc"}) {
            String text = source("observation-forall-constant")
                .replace("channels: c", "channels: c\nenum shade {" + literal + ", dark}");
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ToNuXmv.transform(parse(text)));
            assertTrue(error.getMessage(), error.getMessage().contains("constant '" + literal + "'"));
            assertTrue(error.getMessage(), error.getMessage().contains("symbolic identifier"));
        }
    }

    @Test public void numericSuffixesRemainValid() throws Exception {
        for (String literal : new String[]{"value123", "Value123", "v123"}) {
            String text = source("observation-forall-constant")
                .replace("channels: c", "channels: c\nenum shade {" + literal + ", dark}")
                .replace("value: boolean", "value: shade")
                .replace("value := TRUE", "value := " + literal)
                .replace("receive: <value>", "receive: <value == " + literal + ">")
                .replace("received := value", "received := TRUE")
                .replace("SPEC G (<forall(TRUE)>true);", "");
            assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
        }
    }
}
