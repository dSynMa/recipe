package recipe.analysis;

import org.junit.Test;
import recipe.lang.utils.Parsing;
import java.util.List;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationQualifiedPrefixTest {
    @Test public void qualifiedNamesPreferLongestDeclaredMatchInEitherOrder() {
        for (List<String> names : List.of(
                List.of("sender-automaton", "sender-automaton-state"),
                List.of("sender-automaton-state", "sender-automaton"))) {
            assertEquals("sender-automaton-state", Parsing.disjunctiveStringParser(names)
                .end().parse("sender-automaton-state").get());
            assertEquals("sender-automaton", Parsing.disjunctiveStringParser(names)
                .end().parse("sender-automaton").get());
        }
    }

    @Test public void localAutomatonDoesNotHideControlStateInSpecification() throws Exception {
        String model = source("supplier-locality").replace("sender", "a")
            .replace("sent: boolean", "sent: boolean, automaton: 0..1")
            .replace("init: !sent", "init: !sent & automaton == 1")
            + "\nSPEC a-automaton-state == 0;\n";
        String smv = ToNuXmv.transform(parse(model));
        assertCtl(smv, "a-automaton-state = 0 & a-automaton = 1", true);
    }
}
