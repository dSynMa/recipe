package recipe.analysis;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.petitparser.context.Result;
import recipe.lang.System;
import recipe.lang.expressions.predicate.GuardReference;

import static org.junit.Assert.*;

public class NuXmvTranslationTest {
    @Test
    public void preservesSendReceiveOutput() throws Exception {
        assertSnapshot("send-receive");
    }

    @Test
    public void preservesBroadcastAndObservationOutput() throws Exception {
        assertSnapshot("broadcast-observation");
    }

    @Test
    public void preservesSupplyGetOutput() throws Exception {
        assertSnapshot("supply-get");
    }

    @Test
    public void preservesBroadcastWithoutEnabledReceive() throws Exception {
        assertSnapshot("broadcast-no-enabled-receive");
    }

    @Test
    public void preservesNonListeningReceiver() throws Exception {
        assertSnapshot("receiver-not-listening");
    }

    @Test
    public void preservesMissingMessageGuardCandidateRejection() throws Exception {
        assertSnapshot("missing-message-guard");
    }

    @Test
    public void preservesMissingMessageUpdateCandidateRejection() throws Exception {
        assertSnapshot("missing-message-update");
    }

    @Test
    public void preservesMissingSupplyMessageGuardCandidateRejection() throws Exception {
        assertSnapshot("missing-supply-message-guard");
    }

    @Test
    public void preservesMissingSupplyMessageUpdateCandidateRejection() throws Exception {
        assertSnapshot("missing-supply-message-update");
    }

    @Test
    public void preservesMultipleReceiverComposition() throws Exception {
        assertSnapshot("multiple-receivers");
    }

    @Test
    public void preservesAlternativeGetterPairs() throws Exception {
        assertSnapshot("multiple-getters");
    }

    @Test
    public void preservesUnlabelledPairNamesAcrossInvocations() throws Exception {
        assertSnapshot("unlabelled-supply-get");
    }

    @Test
    public void preservesNamedGetterTargetMatching() throws Exception {
        assertSnapshot("named-getter-target");
    }

    @Test
    public void preservesNoEnabledTransitions() throws Exception {
        assertSnapshot("no-enabled-transitions");
    }

    @Test
    public void rejectsGetSelfAndRestoresGuardResolution() throws Exception {
        boolean previous = GuardReference.resolve;
        try {
            System system = parse("get-self");
            for (boolean enabled : new boolean[] {false, true}) {
                GuardReference.resolve = enabled;
                Exception failure = assertThrows(Exception.class, () -> ToNuXmv.transform(system));
                assertEquals("GET@SELF is not allowed.", failure.getMessage());
                assertEquals(enabled, GuardReference.resolve);
            }
        } finally {
            GuardReference.resolve = previous;
        }
    }

    @Test
    public void restoresGuardResolutionAfterSuccess() throws Exception {
        boolean previous = GuardReference.resolve;
        try {
            System system = parse("send-receive");
            for (boolean enabled : new boolean[] {false, true}) {
                GuardReference.resolve = enabled;
                ToNuXmv.transform(system);
                assertEquals(enabled, GuardReference.resolve);
            }
        } finally {
            GuardReference.resolve = previous;
        }
    }

    @Test
    public void restoresGuardResolutionAfterFailure() throws Exception {
        boolean previous = GuardReference.resolve;
        try {
            System system = parse("send-receive");
            system.setSpecs(null);
            for (boolean enabled : new boolean[] {false, true}) {
                GuardReference.resolve = enabled;
                try {
                    ToNuXmv.transform(system);
                    fail("Expected translation of invalid specifications to fail");
                } catch (NullPointerException expected) {
                    assertEquals(enabled, GuardReference.resolve);
                }
            }
        } finally {
            GuardReference.resolve = previous;
        }
    }

    private void assertSnapshot(String name) throws Exception {
        System system = parse(name);
        String expected = resource(name + ".smv.expected");
        // Golden output for the structured renderer; semantic regressions are checked separately.
        assertEquals(name, expected, ToNuXmv.transform(system));
        assertEquals(name, expected, ToNuXmv.transform(system, true));
    }

    private System parse(String name) throws Exception {
        Result result = System.parser().end().parse(resource(name + ".rcp"));
        assertTrue(result.toString(), result.isSuccess());
        return result.get();
    }

    private String resource(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/recipe/analysis/" + name)) {
            assertNotNull(name, input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
