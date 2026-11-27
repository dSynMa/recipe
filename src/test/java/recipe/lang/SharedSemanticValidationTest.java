package recipe.lang;

import org.junit.Before;
import org.junit.Test;
import recipe.Config;
import recipe.lang.agents.Agent;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.process.Process;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.types.Boolean;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Enum;
import recipe.lang.types.Integer;
import recipe.lang.types.Type;
import recipe.lang.utils.TypingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

/**
 * The parser applies several semantic checks after an action has been built.
 * These checks are exposed as shared methods so deserializers can apply the
 * same constraints to JSON-built models.
 */
public class SharedSemanticValidationTest {

    @Before
    public void resetEnums() {
        Enum.clear();
    }

    @Test
    public void feasibilityConstraintsApplyToBoundedAssignments() throws Exception {
        Enum channel = new Enum(Config.channelLabel, List.of("c"));
        BoundedInteger domain = new BoundedInteger(0, 1);
        Map<String, Expression> update = new HashMap<>();
        update.put("x", new TypedValue<Integer>(Integer.getType(), "2"));

        TypingContext local = new TypingContext();
        local.set("x", domain);

        ReceiveProcess action = new ReceiveProcess("receive", Condition.getTrue(),
            new TypedValue<Enum>(channel, "c"), update);

        Process.applyFeasibilityConstraints(action, local, new TypingContext());

        assertNotEquals(Condition.getTrue(), action.entryCondition());
    }

    @Test
    public void feasibilityConstraintsLeavePlainActionsUnchanged() throws Exception {
        Enum channel = new Enum(Config.channelLabel, List.of("c"));
        ReceiveProcess action = new ReceiveProcess("receive", Condition.getTrue(),
            new TypedValue<Enum>(channel, "c"), new HashMap<>());

        Process.applyFeasibilityConstraints(action, new TypingContext(), new TypingContext());

        assertEquals(Condition.getTrue(), action.entryCondition());
    }

    @Test
    public void localDeclarationsCannotShadowChannelConstants() throws Exception {
        Enum channel = new Enum(Config.channelLabel, List.of("c"));
        TypedVariable<Enum> clash = new TypedVariable<>(channel, "c");

        assertThrows(IllegalArgumentException.class,
            () -> Agent.validateLocalDeclarations(List.of(clash)));

        TypedVariable<Integer> counter = new TypedVariable<>(Integer.getType(), "counter");
        Agent.validateLocalDeclarations(List.of(counter));
    }

    @Test
    public void reservedObservationNamesAreRejected() {
        Map<String, Type> messages = new HashMap<>();
        messages.put("sender", Boolean.getType());

        assertThrows(IllegalArgumentException.class,
            () -> recipe.lang.System.validateReservedNames(messages, new HashMap<>(), new HashMap<>()));

        recipe.lang.System.validateReservedNames(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }
}
