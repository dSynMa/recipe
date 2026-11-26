package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationObservationNamespaceTest {
    private void reject(String text, String name) throws Exception {
        RuntimeException error = assertThrows(RuntimeException.class, () -> parse(text));
        assertTrue(error.toString(), error.toString().contains("reserved") && error.toString().contains(name));
    }

    @Test public void payloadFieldsCannotShadowObservationMetadata() throws Exception {
        for (String name : new String[]{"channel", "sender"}) {
            String text = source("observation-forall-constant").replace("value", name);
            reject(text, name);
        }
    }

    @Test public void commonVariablesCannotShadowObservationMetadata() throws Exception {
        for (String name : new String[]{"channel", "sender"})
            reject(source("observation-forall-constant").replace("ready", name), name);
    }

    @Test public void guardNamesCannotShadowObservationMetadata() throws Exception {
        for (String name : new String[]{"channel", "sender"}) {
            String text = source("observation-forall-constant")
                .replace("agent Sender", "guard " + name + "() := TRUE;\nagent Sender")
                .replace("send: <TRUE>", "send: <" + name + "()>");
            reject(text, name);
        }
    }

    @Test public void metadataPrefixGuardNamesRemainUsableInObservations() throws Exception {
        for (String name : new String[]{"channelCheck", "senderCheck"}) {
            String text = source("observation-forall-constant")
                .replace("agent Sender", "guard " + name + "(flag: boolean) := flag;\nagent Sender")
                .replace("send: <TRUE>", "send: <" + name + "(TRUE)>")
                .replace("forall(TRUE)", name + "(value)");
            assertCtl(ToNuXmv.transform(parse(text)), "EX (receiver-received & obs0)", true);
        }
    }

    @Test public void prefixedNamesPreservePayloadAndChannelObservations() throws Exception {
        for (String name : new String[]{"channelData", "senderData"}) {
            String text = source("observation-forall-constant").replace("value", name)
                .replace("Sender(sender, TRUE)", "Sender(emitter, TRUE)")
                .replace("forall(TRUE)", name + " & channel == c & sender == emitter");
            assertCtl(ToNuXmv.transform(parse(text)), "EX (receiver-received & obs0)", true);
        }
    }
}
