package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationSpecificationBoundaryTest {
    @Test
    public void specTextInsideIdentifiersDoesNotSplitProperties() throws Exception {
        for (String name : new String[]{"SPECimen", "aSPEC", "SPEC"}) {
            String model = source("observation-forall-constant")
                .replace("sent", name)
                .replace("SPEC G (<forall(TRUE)>true);", "SPEC G (sender-" + name + " | !sender-" + name + ");");
            String smv = ToNuXmv.transform(parse(model));
            assertLtl(smv, "G (sender-" + name + " | !sender-" + name + ")", true);
        }
    }

    @Test
    public void whitespaceAfterSpecKeywordIsNotLimitedToSpace() throws Exception {
        for (String separator : new String[]{"\t", "\n", "\r\n"}) {
            String model = source("observation-forall-constant").replace("SPEC G", "SPEC" + separator + "G");
            assertValid(ToNuXmv.transform(parse(model)));
        }
    }

    @Test
    public void adjacentPropertiesRetainTheirCount() throws Exception {
        for (String separator : new String[]{"\n", ";\n", "; "}) {
            String model = source("observation-forall-constant")
                .replace("SPEC G (<forall(TRUE)>true);", "SPEC G (sender-sent | !sender-sent)" + separator + "SPEC F sender-sent;");
            String smv = ToNuXmv.transform(parse(model));
            assertEquals(2, smv.lines().filter(line -> line.startsWith("LTLSPEC ")).count());
            assertLtl(smv, "F sender-sent", true);
        }
    }

    @Test
    public void guardNamedSpecRemainsPartOfFormula() throws Exception {
        String model = source("observation-forall-constant")
            .replace("agent Sender", "guard SPEC(flag: boolean) := flag;\nagent Sender")
            .replace("SPEC G (<forall(TRUE)>true);", "SPEC G SPEC (TRUE)\nSPEC F sender-sent;");
        String smv = ToNuXmv.transform(parse(model));
        assertEquals(2, smv.lines().filter(line -> line.startsWith("LTLSPEC ")).count());
        assertValid(smv);
    }

    @Test
    public void commonVariableNamedSpecRemainsInsideObservation() throws Exception {
        String model = source("observation-forall-constant")
            .replace("ready", "SPEC")
            .replace("SPEC G (<forall(TRUE)>true);", "SPEC G (<exists(SPEC )>true);");
        assertValid(ToNuXmv.transform(parse(model)));
    }
}
