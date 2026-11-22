package recipe.analysis;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.*;
import recipe.lang.store.ConcreteStore;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Number;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationNumericDomainSoundnessTest {
    private final TypedVariable x = new TypedVariable(new BoundedInteger(-2, 2), "x");
    private final TypedVariable y = new TypedVariable(new BoundedInteger(0, 2), "y");

    private Expression literal(String value) throws Exception {
        return new TypedValue(recipe.lang.types.Real.getType(), value);
    }

    private Expression tree(Random random, int depth) throws Exception {
        if (depth == 0) {
            switch (random.nextInt(4)) {
                case 0: return x;
                case 1: return y;
                case 2: return literal("0.5");
                default: return literal(Integer.toString(random.nextInt(5) - 2));
            }
        }
        Expression lhs = tree(random, depth - 1);
        switch (random.nextInt(4)) {
            case 0: return new Addition(lhs, tree(random, depth - 1));
            case 1: return new Subtraction(lhs, tree(random, depth - 1));
            case 2: return new Multiplication(lhs, tree(random, depth - 1));
            default: return new Division(lhs, literal(new String[]{"-1", "1", "2"}[random.nextInt(3)]));
        }
    }

    @Test public void acceptedSmallDomainTreesStayWithinTheirClaimedDomain() throws Exception {
        Random random = new Random(87921);
        int accepted = 0;
        BoundedInteger[] targets = {new BoundedInteger(-16, 16), new BoundedInteger(-2, 2), new BoundedInteger(0, 1)};
        for (int sample = 0; sample < 300; sample++) {
            Expression expression = tree(random, 2).simplify();
            for (BoundedInteger target : targets) {
                if (!NumericDomain.isSubsetOf(expression, target)) continue;
                accepted++;
                for (int xv = -2; xv <= 2; xv++) for (int yv = 0; yv <= 2; yv++) {
                    Map<TypedVariable, TypedValue> values = new HashMap<>();
                    values.put(x, new TypedValue(x.getType(), Integer.toString(xv)));
                    values.put(y, new TypedValue(y.getType(), Integer.toString(yv)));
                    BigDecimal result = new BigDecimal(expression.valueIn(new ConcreteStore(values)).getValue().toString());
                    assertTrue(expression.toString(), result.stripTrailingZeros().scale() <= 0);
                    assertTrue(expression.toString(), result.compareTo(BigDecimal.valueOf(target.getMin())) >= 0);
                    assertTrue(expression.toString(), result.compareTo(BigDecimal.valueOf(target.getMax())) <= 0);
                }
            }
        }
        assertTrue("Expected substantial accepted coverage, got " + accepted, accepted >= 100);
    }

    @Test public void intervalArithmeticCannotWrapIntoASmallerDestination() throws Exception {
        Expression huge = new TypedVariable(new BoundedInteger(Integer.MAX_VALUE - 1, Integer.MAX_VALUE), "huge");
        BoundedInteger target = new BoundedInteger(0, 1);
        assertFalse(NumericDomain.isSubsetOf(new Addition(huge, literal("2")), target));
        assertFalse(NumericDomain.isSubsetOf(new Multiplication(huge, huge), target));
        assertTrue(NumericDomain.isSubsetOf(new Subtraction(huge, literal("2147483646")), target));
    }

    @Test public void nonlinearSafeMappingAgreesWithReceiverSelectionAndObservation() throws Exception {
        String text = "channels: c\nmessage-structure: value: 0..4\ncommunication-variables: rank: 0..4\n"
            + "agent Sender\nlocal: pivot: 0..4, sent: boolean\ninit: !sent\nrelabel: rank <- 0\nreceive-guard: FALSE\n"
            + "repeat: send: <!sent> *! (@rank == pivot)(value := pivot)[sent := TRUE]\n"
            + "agent Receiver\nlocal: score: 0..2, received: boolean\ninit: !received\n"
            + "relabel: rank <- (score - 2) * (score - 2)\nreceive-guard: FALSE\n"
            + "repeat: receive: <TRUE> *? [received := TRUE]\n"
            + "system = Sender(sender, TRUE) | Receiver(receiver, TRUE)\nSPEC G (<exists(rank == value)>true);\n";
        String output = ToNuXmv.transform(parse(text));
        assertCtl(output, "AX obs0", true);
        assertCtl(output, "AX (receiver-received <-> ((receiver-score - 2) * (receiver-score - 2) = sender-pivot))", true);
    }
}
