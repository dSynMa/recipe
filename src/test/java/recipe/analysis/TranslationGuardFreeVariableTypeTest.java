package recipe.analysis;

import org.junit.Test;
import recipe.lang.definitions.GuardDefinition;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Guard;
import recipe.lang.types.Real;
import recipe.lang.types.Type;
import recipe.lang.utils.TypingContext;

import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationGuardFreeVariableTypeTest {
    @Test
    public void freeChannelVariableCannotChangeTypeAtCallSite() throws Exception {
        for (String type : new String[]{"boolean", "0..1", "shade"}) {
            String text = source("observation-forall-constant")
                .replace("channels: c", "channels: c\nenum shade {c, dark}")
                .replace("agent Sender", "guard onC() := channel == c;\nagent Sender")
                .replace("local: sent: boolean", "local: sent: boolean, channel: " + type)
                .replace("c! (@ready)", "c! (@ready & onC())");
            assertThrows(IllegalArgumentException.class, () -> parse(text));
        }
    }

    @Test
    public void sameTypedLocalChannelKeepsItsValueInMacro() throws Exception {
        String text = source("observation-forall-constant").replace("channels: c", "channels: c, d")
            .replace("agent Sender", "guard onD() := channel == d;\nagent Sender")
            .replace("local: sent: boolean", "local: sent: boolean, channel: channel")
            .replace("init: !sent", "init: !sent & channel == d")
            .replace("c! (@ready)", "c! (@ready & onD())");
        assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
    }

    @Test
    public void substitutedNumericArgumentsKeepTheirCallerTypes() throws Exception {
        String text = source("observation-forall-constant")
            .replace("agent Sender", "guard positive(n: real) := n > 0;\nguard identity(flag: boolean) := flag;\nagent Sender")
            .replace("local: sent: boolean", "local: sent: boolean, count: 0..1")
            .replace("init: !sent", "init: !sent & count == 1")
            .replace("send: <TRUE>", "send: <identity(positive(count))>");
        assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
    }

    @Test
    public void explicitBooleanArgumentCanUseLocalChannel() throws Exception {
        String text = source("observation-forall-constant")
            .replace("agent Sender", "guard identity(flag: boolean) := flag;\nagent Sender")
            .replace("local: sent: boolean", "local: sent: boolean, channel: boolean")
            .replace("init: !sent", "init: !sent & channel")
            .replace("send: <TRUE>", "send: <identity(channel)>");
        assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
    }

    @Test
    public void freeNumericDomainsMayNarrowButCannotWiden() throws Exception {
        Type bounded = new BoundedInteger(0, 2);
        assertTrue(parseFreeDomain(bounded, new BoundedInteger(0, 1)));
        assertThrows(IllegalArgumentException.class, () -> parseFreeDomain(bounded, new BoundedInteger(0, 3)));
        assertTrue(parseFreeDomain(Real.getType(), recipe.lang.types.Integer.getType()));
        assertThrows(IllegalArgumentException.class, () -> parseFreeDomain(recipe.lang.types.Integer.getType(), Real.getType()));
    }

    private boolean parseFreeDomain(Type expected, Type actual) throws Exception {
        TypedVariable variable = new TypedVariable(expected, "free");
        Guard guard = new Guard("freeDomainAudit", new TypedVariable[0]);
        Guard.setDefinition(guard.name(), new GuardDefinition(guard,
            new IsEqualTo(variable, variable)));
        TypingContext types = new TypingContext();
        types.set("free", actual);
        types.set(guard.name(), guard);
        return Condition.parser(types).end().parse("freeDomainAudit()").isSuccess();
    }
}
