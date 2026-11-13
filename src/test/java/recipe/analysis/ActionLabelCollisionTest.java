package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ActionLabelCollisionTest {
    private void assertRejectedLabel(String label) throws Exception {
        recipe.lang.System model = parse(source("supplier-locality").replace("send: <TRUE>", label + ": <TRUE>"));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ToNuXmv.transform(model));
        assertTrue(error.getMessage(), error.getMessage().contains(label));
    }

    @Test
    public void labelCannotAliasLocalState() throws Exception {
        assertRejectedLabel("sent");
    }

    @Test
    public void labelCannotAliasSyntheticIdentity() throws Exception {
        assertRejectedLabel("name");
    }
}
