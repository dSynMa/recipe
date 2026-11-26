package recipe.analysis;

import org.junit.Test;
import recipe.lang.types.BoundedInteger;
import recipe.lang.expressions.TypedValue;
import java.util.Set;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class BoundedIntegerBoundaryTest {
    @Test
    public void enumerationIncludesExactlyTheValuesAtBothIntegerLimits() throws Exception {
        for (int[] range : new int[][] {{2147483647, 2147483647}, {2147483645, 2147483647},
                {-2147483648, -2147483646}, {-1, 1}}) {
            BoundedInteger type = new BoundedInteger(range[0], range[1]);
            Set<TypedValue> values = type.getAllValues();
            assertEquals((long) range[1] - range[0] + 1, values.size());
            for (long i = range[0]; i <= range[1]; i++)
                assertTrue(values.contains(new TypedValue<>(type, Long.toString(i))));
        }
    }

    @Test
    public void onlyTranslationRespondsToBuildCancellation() throws Exception {
        BoundedInteger type = new BoundedInteger(1, 2);
        Thread.currentThread().interrupt();
        try {
            assertEquals(2, type.getAllValues().size());
            assertThrows(java.util.concurrent.CancellationException.class, () ->
                ToNuXmv.specialiseOnAllPossibleValues("x", type, recipe.lang.expressions.predicate.Condition.getTrue()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void valueParserTerminatesAtTheUpperIntegerLimit() throws Exception {
        BoundedInteger type = new BoundedInteger(2147483646, 2147483647);
        org.petitparser.parser.Parser parser = type.valueParser().end();
        assertTrue(parser.parse("2147483646").isSuccess());
        assertTrue(parser.parse("2147483647").isSuccess());
        assertTrue(parser.parse("2147483648").isFailure());
        assertTrue(parser.parse("-2147483648").isFailure());
    }

    @Test(timeout = 2000)
    public void valueParserHandlesTheFullIntegerDomainWithoutEnumeratingIt() {
        BoundedInteger type = new BoundedInteger(java.lang.Integer.MIN_VALUE, java.lang.Integer.MAX_VALUE);
        org.petitparser.parser.Parser parser = type.valueParser().end();
        for (String literal : new String[] {"-2147483648", "-1", "0", "1", "2147483647"}) {
            org.petitparser.context.Result result = parser.parse(literal);
            assertTrue(literal + ": " + result, result.isSuccess());
            TypedValue<?> value = result.get();
            assertEquals(type, value.getType());
            assertEquals(java.lang.Integer.parseInt(literal), value.getValue());
        }
        for (String literal : new String[] {"-2147483649", "2147483648", "999999999999999999999",
                "", "-", "1.5", "1e2", "12name", "12_name"}) {
            assertTrue(literal, parser.parse(literal).isFailure());
        }
        assertTrue(type.valueParser().parse("12name").isFailure());
    }

    @Test
    public void outOfRangeValuesFailAndAllowAlternativeParsers() {
        BoundedInteger first = new BoundedInteger(-2, 2);
        BoundedInteger second = new BoundedInteger(3, 5);
        org.petitparser.parser.Parser parser = first.valueParser().or(second.valueParser()).end();
        assertTrue(first.valueParser().end().parse("3").isFailure());
        assertTrue(first.valueParser().end().parse("-3").isFailure());
        org.petitparser.context.Result result = parser.parse("4");
        assertTrue(result.toString(), result.isSuccess());
        TypedValue<?> value = result.get();
        assertEquals(second, value.getType());
        assertEquals(4, value.getValue());
    }

    @Test
    public void observationsQuantifyOverTheIntegerLimit() throws Exception {
        String model = source("observation-forall-constant")
            .replace("communication-variables: ready: boolean", "communication-variables: ready: 2147483647..2147483647")
            .replace("ready <- TRUE", "ready <- 2147483647")
            .replace("ready <- !received", "ready <- 2147483647")
            .replace("(@ready)", "(@ready == 2147483647)");
        for (String predicate : new String[] {"forall", "exists"}) {
            String output = ToNuXmv.transform(parse(model.replace("forall(TRUE)", predicate + "(ready == 2147483647)")));
            assertValid(output);
            assertCtl(output, "EX obs0", true);
        }
    }
}
