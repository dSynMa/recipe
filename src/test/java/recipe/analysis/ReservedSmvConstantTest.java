package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReservedSmvConstantTest {
    @Test
    public void reservedAgentNamesFailBeforeEmittingInvalidSmv() throws Exception {
        for (String name : new String[] {"F", "G", "X", "MODULE", "next"}) {
            assertRejected(source("supplier-locality").replace("Sender(sender,", "Sender(" + name + ","), name);
        }
    }

    @Test
    public void reservedChannelsAndGeneratedGlobalsAreRejected() throws Exception {
        for (String name : new String[] {"count", "boolean", "progress", "transition", "obs0"}) {
            String model = source("supplier-locality")
                .replace("channels: c", "channels: " + name)
                .replace("channel == c", "channel == " + name);
            assertRejected(model, name);
        }
    }

    @Test
    public void caseSensitiveNamesAndKeywordPrefixesRemainValid() throws Exception {
        for (String name : new String[] {"module", "Next", "Fsender", "progressive", "observe"}) {
            assertValid(ToNuXmv.transform(parse(source("supplier-locality")
                .replace("Sender(sender,", "Sender(" + name + ","))));
        }
    }

    @Test
    public void newerAndTimedKeywordsAreRejectedInEnumDeclarations() throws Exception {
        for (String name : new String[] {"acos", "asin", "atan", "at_next", "at_last", "sqrt", "Clock", "clock", "time"}) {
            assertRejected(source("supplier-locality")
                .replace("message-structure:", "enum choice {" + name + ", ordinary}\nmessage-structure:"), name);
        }
    }

    private void assertRejected(String model, String name) throws Exception {
        try {
            assertFalse("Accepted reserved name: " + name,
                recipe.lang.System.parser().end().parse(model).isSuccess());
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage(), error.getMessage().contains("constant '" + name + "'"));
            assertTrue(error.getMessage(), error.getMessage().contains("reserved"));
        }
    }
}
