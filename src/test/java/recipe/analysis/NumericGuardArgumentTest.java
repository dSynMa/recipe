package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.*;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class NumericGuardArgumentTest {
    private boolean accepts(Type target, Type source) {
        return new TypedVariable<>(source, "argument").isValidAssignmentFor(new TypedVariable<>(target, "parameter"));
    }

    @Test
    public void numericVariablesOnlyWidenToCompatibleDomains() {
        Type integer = recipe.lang.types.Integer.getType();
        Type real = Real.getType();
        Type small = new BoundedInteger(1, 2), large = new BoundedInteger(0, 8);
        assertTrue(accepts(real, integer));
        assertTrue(accepts(real, small));
        assertTrue(accepts(integer, small));
        assertTrue(accepts(large, small));
        assertTrue(accepts(small, small));
        assertFalse(accepts(integer, real));
        assertFalse(accepts(small, integer));
        assertFalse(accepts(small, real));
        assertFalse(accepts(small, large));
        assertFalse(accepts(real, recipe.lang.types.Boolean.getType()));
    }

    @Test
    public void boundedIntegerArgumentEnablesTheSameSendAsAnIntegerLiteral() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("agent Sender", "guard positive(v: real) := v > 0;\nagent Sender")
            .replace("count: integer", "count: 0..8")
            .replace("send: <TRUE>", "send: <count == 2 & positive(count)>");
        assertCtl(ToNuXmv.transform(parse(model)), "EX sender-sent", true);
    }

    @Test
    public void integerVariableCanBeUsedInARealGuardInASpecification() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("agent Sender", "guard positive(v: real) := v > 0;\nagent Sender");
        String output = ToNuXmv.transform(parse(model + "\nSPEC positive(sender-count);\n"));
        assertValid(output);
        assertSmtInvariant(output, "sender-count >= 2");
    }
}
