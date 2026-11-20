package recipe.analysis;

import org.junit.Test;
import recipe.lang.System;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;

import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SupplyGetStateTest {
    @Test
    public void eachGetterUsesItsOwnSourceState() throws Exception {
        System model = parse(source("getter-state"));
        String output = ToNuXmv.transform(model);
        int checked = 0;
        for (AgentInstance instance : model.getAgentInstances()) {
            for (ProcessTransition transition : instance.getAgent().getGetTransitions()) {
                String name = "sender-send-" + instance.getLabel() + "-" + transition.getLabel().getLabel();
                String sourceCondition = "(" + instance.getLabel() + "-automaton-state = "
                    + transition.getSource() + " &";
                assertTrue(name, definition(output, name).contains(sourceCondition));
                assertTrue(name + "-progress", definition(output, name + "-progress").contains(sourceCondition));
                checked++;
            }
        }
        assertEquals(2, checked);
    }

    @Test
    public void secondGetRemainsEnabledAfterFirstGet() throws Exception {
        String output = ToNuXmv.transform(parse(source("getter-state")));
        assertCtl(output, "EX (receiver-automaton-state = 1)", true);
        assertCtl(output, "AG (receiver-automaton-state = 1 -> EX (receiver-automaton-state = 0))", true);
    }
}
