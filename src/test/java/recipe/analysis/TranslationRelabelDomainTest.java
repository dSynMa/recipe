package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationRelabelDomainTest {
    private String model(String mapping) {
        return "channels: c\nmessage-structure: value: boolean\n"
            + "communication-variables: rank: 0..1\n"
            + "agent Sender\nlocal: sent: boolean\ninit: !sent\n"
            + "relabel: rank <- 0\nreceive-guard: FALSE\n"
            + "repeat: send: <!sent> *! (@rank == 2)(value := TRUE)[sent := TRUE]\n"
            + "agent Receiver\nlocal: received: boolean\ninit: !received\n"
            + "relabel: rank <- " + mapping + "\nreceive-guard: FALSE\n"
            + "repeat: receive: <TRUE> *? [received := TRUE]\n"
            + "system = Sender(sender, TRUE) | Receiver(receiver, TRUE)\n"
            + "SPEC G (<exists(TRUE)>true);\n";
    }
    @Test public void constantMappingsCannotEscapeCommonVariableDomain() throws Exception {
        for (String mapping : new String[]{"2", "0 - 1", "0.5", "1 + 1", "3 / 2"}) {
            assertThrows(IllegalArgumentException.class, () -> parse(model(mapping)));
        }
    }
    @Test public void legalMappingsDoNotReceiveImpossibleSelection() throws Exception {
        for (String mapping : new String[]{"0", "1", "1.0", "1 + 0", "3 / 3"})
            assertCtl(ToNuXmv.transform(parse(model(mapping))), "AX !receiver-received", true);
    }
    @Test public void integerMappingsRequireIntegralConstants() throws Exception {
        String text = model("0.5").replace("rank: 0..1", "rank: integer").replace("SPEC G (<exists(TRUE)>true);", "");
        assertThrows(IllegalArgumentException.class, () -> parse(text));
        parse(text.replace("rank <- 0.5", "rank <- 1.0"));
    }
    @Test public void widerSymbolicDomainsAreRejectedEvenWithRestrictiveInit() throws Exception {
        String text = model("score")
            .replace("local: received: boolean", "local: received: boolean, score: 0..2")
            .replace("init: !received", "init: !received & score == 1")
            .replace("@rank == 2", "@rank == 1");
        assertThrows(IllegalArgumentException.class, () -> parse(text));
        assertCtl(ToNuXmv.transform(parse(text.replace("score: 0..2", "score: 0..1"))),
            "AX (receiver-received & obs0)", true);
    }
    @Test public void symbolicExpressionsMustFitForTheirEntireDeclaredDomain() throws Exception {
        String base = model("score").replace("local: received: boolean", "local: received: boolean, score: 0..1")
            .replace("init: !received", "init: !received & score == 1");
        for (String expression : new String[]{"score + 1", "score - 1", "score * 2", "score / 2"}) {
            String text = base.replace("rank <- score", "rank <- " + expression);
            assertThrows(IllegalArgumentException.class, () -> parse(text));
        }
        for (String expression : new String[]{"score + 0", "1 - score", "score * 1", "score / 1", "(score - 1) * (score - 1)"})
            parse(base.replace("rank <- score", "rank <- " + expression));
        for (String type : new String[]{"integer", "real", "0..2"}) {
            String text = base.replace("score: 0..1", "score: " + type);
            assertThrows(IllegalArgumentException.class, () -> parse(text));
        }
        parse(base.replace("score: 0..1", "score: 1..1"));
    }

    @Test public void unboundedIntegerTargetsStillRequireIntegralSources() throws Exception {
        String base = model("score").replace("rank: 0..1", "rank: integer")
            .replace("local: received: boolean", "local: received: boolean, score: integer")
            .replace("SPEC G (<exists(TRUE)>true);", "");
        parse(base);
        parse(base.replace("rank <- score", "rank <- score + 1"));
        assertThrows(IllegalArgumentException.class, () -> parse(base.replace("score: integer", "score: real")));
        assertThrows(IllegalArgumentException.class, () -> parse(base.replace("rank <- score", "rank <- score / 2")));
    }

    @Test public void legalIntegralConstantsMatchSymbolicSenderValues() throws Exception {
        for (String mapping : new String[]{"1", "1.0", "1 + 0", "3 / 3"}) {
            String text = model(mapping).replace("local: sent: boolean", "local: sent: boolean, pivot: 0..1")
                .replace("init: !sent", "init: !sent & pivot == 1")
                .replace("@rank == 2", "@rank == pivot");
            assertCtl(ToNuXmv.transform(parse(text)), "AX (receiver-received & obs0)", true);
        }
    }
}
