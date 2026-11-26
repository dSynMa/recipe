package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SyntheticIdentityCollisionTest {
    @Test
    public void localNameCannotBeSilentlyReplacedByAgentIdentity() throws Exception {
        String model = source("supplier-locality")
            .replace("sent: boolean", "sent: boolean, name: boolean")
            .replace("init: !sent", "init: !sent & !name");
        RuntimeException error = assertThrows(RuntimeException.class, () -> parse(model));
        assertTrue(error.getMessage(), error.getMessage().contains("name"));
        assertTrue(error.getMessage(), error.getMessage().contains("reserved"));
    }

    @Test
    public void similarLocalNamesRemainAvailable() throws Exception {
        String model = source("supplier-locality")
            .replace("sent: boolean", "sent: boolean, nickname: boolean")
            .replace("init: !sent", "init: !sent & !nickname");
        assertValid(ToNuXmv.transform(parse(model)));
    }
}
