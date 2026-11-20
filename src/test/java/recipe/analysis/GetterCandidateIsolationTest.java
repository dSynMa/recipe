package recipe.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import recipe.lang.System;
import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.process.BasicProcess;

import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class GetterCandidateIsolationTest {
    @Test
    public void missingGuardFieldRejectsOnlyThatCandidateInEitherOrder() throws Exception {
        assertCandidateIsolation(source("getter-candidate-isolation"));
    }

    @Test
    public void missingUpdateFieldRejectsOnlyThatCandidateInEitherOrder() throws Exception {
        assertCandidateIsolation(source("getter-candidate-isolation")
            .replace("invalid: <absent> GET@TRUE [received := FALSE]",
                     "invalid: <value> GET@TRUE [received := absent]"));
    }

    @Test
    public void validGetCanExecuteAfterRejectedCandidate() throws Exception {
        System model = withCandidateOrder(source("getter-candidate-isolation"), true);
        assertCtl(ToNuXmv.transform(model), "EX receiver-received", true);
    }

    private void assertCandidateIsolation(String source) throws Exception {
        for (boolean invalidFirst : new boolean[] {true, false}) {
            String output = ToNuXmv.transform(withCandidateOrder(source, invalidFirst));
            assertTrue("Valid get missing; invalidFirst=" + invalidFirst,
                output.contains("sender-send-receiver-valid :="));
            assertFalse(output.contains("sender-send-receiver-invalid :="));
        }
    }

    private System withCandidateOrder(String source, boolean invalidFirst) throws Exception {
        System model = parse(source);
        for (int i = 0; i < model.getAgentInstances().size(); i++) {
            AgentInstance instance = model.getAgentInstances().get(i);
            if (!instance.getLabel().equals("receiver")) continue;
            Agent original = instance.getAgent();
            Agent ordered = new Agent(original.getName(), original.getStore(), original.getInit(),
                original.getStates(), original.getSendTransitions(), original.getReceiveTransitions(),
                original.getActions(), original.getInitialState(), original.getReceiveGuard(),
                original.getGetTransitions(), original.getSupplyTransitions()) {
                @Override
                public <T extends Transition> Map<State, Set<T>> getStateTransitionMap(Set<T> transitions) {
                    Map<State, Set<T>> result = super.getStateTransitionMap(transitions);
                    result.replaceAll((state, candidates) -> {
                        List<T> sorted = new ArrayList<>(candidates);
                        sorted.sort(Comparator.comparingInt(t -> {
                            boolean invalid = "invalid".equals(((BasicProcess) t.getLabel()).getLabel());
                            return invalid == invalidFirst ? 0 : 1;
                        }));
                        return new LinkedHashSet<>(sorted);
                    });
                    return result;
                }
            };
            ordered.setRelabel(original.getRelabel());
            model.getAgents().remove(original);
            model.getAgents().add(ordered);
            model.getAgentInstances().set(i, new AgentInstance(instance.getLabel(), instance.getInit(), ordered));
            return model;
        }
        throw new AssertionError("Missing receiver in fixture");
    }
}
