package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.arithmetic.Multiplication;
import recipe.lang.expressions.arithmetic.Division;
import recipe.lang.types.Number;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class NumericEqualityTest {
    @Test
    public void equalNumbersHaveEqualHashesAcrossNumericTypesAndUnions() throws Exception {
        TypedValue<?> integer = new TypedValue<>(recipe.lang.types.Integer.getType(), "2");
        TypedValue<?> real = new TypedValue<>(recipe.lang.types.Real.getType(), "2.0");
        TypedValue<?> bounded = new TypedValue<>(new recipe.lang.types.BoundedInteger(0, 4), "2");
        TypedValue<?> union = new TypedValue<>(new recipe.lang.types.UnionType(
            java.util.List.of(recipe.lang.types.Integer.getType())), "2");
        for (TypedValue<?> left : java.util.List.of(integer, real, bounded, union)) {
            for (TypedValue<?> right : java.util.List.of(integer, real, bounded, union)) {
                assertEquals(left, right);
                assertEquals(left.hashCode(), right.hashCode());
            }
        }
        assertEquals(1, new java.util.HashSet<>(java.util.List.of(integer, real, bounded, union)).size());
        assertNotEquals(integer, new TypedValue<>(recipe.lang.types.Real.getType(), "2.5"));
        assertNotEquals(integer, new TypedValue<>(recipe.lang.types.Boolean.getType(), "TRUE"));
        assertEquals(new TypedValue<>(recipe.lang.types.Integer.getType(), "0"),
            new TypedValue<>(recipe.lang.types.Real.getType(), "-0.0"));
    }
    @Test
    public void foldingArithmeticPreservesNumericEquality() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..8")
            .replace("send: <TRUE>", "send: <count == 2 & (1 + 1 == 2)>");
        assertCtl(ToNuXmv.transform(parse(model.replace("1 + 1", "2"))), "EX sender-sent", true);
        assertCtl(ToNuXmv.transform(parse(model)), "EX sender-sent", true);
    }

    @Test
    public void foldingArithmeticDoesNotEnableFalseInequality() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..8")
            .replace("send: <TRUE>", "send: <count == 2 & (1 + 1 != 2)>");
        assertCtl(ToNuXmv.transform(parse(model.replace("1 + 1", "2"))), "EX sender-sent", false);
        assertCtl(ToNuXmv.transform(parse(model)), "EX sender-sent", false);
    }

}
