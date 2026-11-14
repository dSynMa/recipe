package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;
import static org.junit.Assert.*;
import recipe.lang.utils.Parsing;

public class TranslationNumericTypeBoundaryTest {
    @Test
    public void numericKeywordPrefixesDoNotHideEnumTypes() throws Exception {
        for (String name : new String[]{"integerish", "intensity", "reality"}) {
            String model = source("observation-forall-constant")
                .replace("channels: c", "channels: c\nenum " + name + " {low, high}")
                .replace("local: sent: boolean", "local: sent: boolean, level: " + name)
                .replace("init: !sent", "init: !sent & level == low")
                .replace("[sent := TRUE]", "[sent := TRUE, level := high]");
            assertCtl(ToNuXmv.transform(parse(model)), "EX (sender-level = high)", true);
        }
    }

    @Test
    public void prefixedEnumTypesWorkInGuardSignatures() throws Exception {
        String model = source("observation-forall-constant")
            .replace("channels: c", "channels: c\nenum intensity {low, high}")
            .replace("agent Sender", "guard isHigh(level: intensity) := level == high;\nagent Sender")
            .replace("send: <TRUE>", "send: <isHigh(high)>");
        assertCtl(ToNuXmv.transform(parse(model)), "EX receiver-received", true);
    }

    @Test
    public void builtInNumericTypesKeepCaseInsensitiveSpelling() {
        for (String name : new String[]{"integer", "INTEGER", "int", "Int", "real", "REAL", "0..2"}) {
            assertTrue(name, Parsing.numberType().end().parse(name).isSuccess());
        }
        for (String name : new String[]{"integerish", "intensity", "reality"}) {
            assertTrue(name, Parsing.numberType().parse(name).isFailure());
        }
    }
}
