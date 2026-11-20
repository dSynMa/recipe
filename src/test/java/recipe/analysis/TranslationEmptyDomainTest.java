package recipe.analysis;

import org.junit.Test;
import recipe.lang.types.BoundedInteger;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationEmptyDomainTest {
    @Test public void invertedBoundsAreRejectedAtTypeConstruction() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> new BoundedInteger(2, 1));
        assertTrue(error.getMessage(), error.getMessage().contains("2..1"));
    }

    @Test public void unusedEmptyMessageDomainsAreRejectedWhileParsing() throws Exception {
        String text = source("observation-forall-constant")
            .replace("message-structure: value: boolean", "message-structure: value: boolean, unused: 2..1");
        assertThrows(IllegalArgumentException.class, () -> parse(text));
    }

    @Test public void emptyLocalDomainsAreRejectedBeforeRendering() throws Exception {
        String text = source("observation-forall-constant")
            .replace("local: sent: boolean", "local: sent: boolean, count: 2..1");
        assertThrows(IllegalArgumentException.class, () -> parse(text));
    }

    @Test public void singletonLocalDomainsRemainValid() throws Exception {
        String text = source("observation-forall-constant")
            .replace("local: sent: boolean", "local: sent: boolean, count: 1..1");
        assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
    }
}
