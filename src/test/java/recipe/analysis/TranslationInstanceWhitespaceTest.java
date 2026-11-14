package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationInstanceWhitespaceTest {
    @Test
    public void instanceNamesExcludeTrailingWhitespace() throws Exception {
        for (String space : new String[]{" ", "\t", "\n"}) {
            recipe.lang.System system = parse(source("supplier-locality")
                .replace("sender,", "sender" + space + ",")
                .replace("first,", "first" + space + ","));
            assertTrue(system.getAgentInstances().stream().anyMatch(i -> i.getLabel().equals("sender")));
            assertTrue(system.getAgentInstances().stream().anyMatch(i -> i.getLabel().equals("first")));
        }
    }

    @Test
    public void spacedInstanceNamesPreserveNamedRendezvous() throws Exception {
        String model = source("supplier-locality")
            .replace("GET@TRUE", "GET@sender")
            .replace("sender,", "sender ,")
            .replace("first,", "first ,");
        assertCtl(ToNuXmv.transform(parse(model)), "EX first-received", true);
        assertCtl(ToNuXmv.transform(parse(model)), "EX second-received", false);
    }
}
