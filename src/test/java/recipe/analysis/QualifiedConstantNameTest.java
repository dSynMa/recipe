package recipe.analysis;

import org.junit.Test;
import recipe.lang.utils.Parsing;
import static org.junit.Assert.*;

public class QualifiedConstantNameTest {
    @Test
    public void hyphenatedConstantsCannotAliasQualifiedReferences() {
        for (String name : new String[] {"sender-", "sender-count", "sender-automaton-state"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Parsing.constantName(name));
            assertTrue(error.getMessage().contains("hyphens are reserved"));
        }
        assertEquals("sender_count", Parsing.constantName("sender_count"));
    }
}
