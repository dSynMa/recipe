package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.Predicate;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Enum;
import recipe.lang.types.Real;
import recipe.lang.types.Type;
import recipe.lang.types.UnionType;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class TypeCompatibilityTest {
    @Test
    public void unionsRequireEverySourceMemberToFit() {
        Type integer = recipe.lang.types.Integer.getType();
        Type bool = recipe.lang.types.Boolean.getType();
        UnionType numbers = new UnionType(List.of(integer, Real.getType()));
        UnionType mixed = new UnionType(List.of(integer, bool));
        assertTrue(numbers.accepts(integer));
        assertTrue(Real.getType().accepts(numbers));
        assertFalse(numbers.accepts(mixed));
        assertFalse(mixed.accepts(numbers));
        assertFalse(new TypedVariable<>(mixed, "source")
                .isValidAssignmentFor(new TypedVariable<>(numbers, "target")));
        assertTrue(new UnionType(List.of(numbers, bool)).accepts(mixed));
    }

    @Test
    public void contextsAndTheirComplementsUseDomainCompatibility() {
        Type narrow = new BoundedInteger(1, 2);
        Type wide = new BoundedInteger(0, 8);
        TypingContext context = new TypingContext();
        context.set("small", narrow);
        context.set("large", wide);
        assertEquals(Set.of("small"), context.get(narrow));
        assertEquals(Set.of("large"), context.getComplementSubContext(narrow).getVarType().keySet());

        Type bool = recipe.lang.types.Boolean.getType();
        UnionType target = new UnionType(List.of(wide, bool));
        context.set("compatible", new UnionType(List.of(narrow, bool)));
        context.set("incompatible", new UnionType(List.of(Real.getType(), bool)));
        assertEquals(Set.of("small", "large", "compatible"), context.get(target));
        assertEquals(Set.of("incompatible"), context.getComplementSubContext(target).getVarType().keySet());
    }

    @Test
    public void namedEnumsRemainDistinctEvenWhenLiteralSpellingsMatch() throws Exception {
        Enum.clear();
        Enum first = new Enum("firstKind", List.of("false", "shared"));
        Enum second = new Enum("secondKind", List.of("false", "shared"));
        assertFalse(first.accepts(second));
        assertFalse(new TypedValue<>(second, "shared")
                .isValidAssignmentFor(new TypedVariable<>(first, "target")));
        assertFalse(new TypedValue<>(first, "false")
                .isValidAssignmentFor(new TypedVariable<>(recipe.lang.types.Boolean.getType(), "flag")));
        assertFalse(Condition.getFalse().isValidAssignmentFor(new TypedVariable<>(first, "target")));
        UnionType both = new UnionType(List.of(first, second));
        assertTrue(both.accepts(first));
        assertTrue(new TypedValue<>(both, "shared")
                .isValidAssignmentFor(new TypedVariable<>(first, "target")));
    }

    @Test
    public void literalNarrowingRemainsValueBased() throws Exception {
        TypedVariable<BoundedInteger> target = new TypedVariable<>(new BoundedInteger(0, 2), "target");
        assertFalse(target.getType().accepts(Real.getType()));
        assertTrue(new TypedValue<>(Real.getType(), "2.0").isValidAssignmentFor(target));
        assertFalse(new TypedValue<>(Real.getType(), "2.5").isValidAssignmentFor(target));
        assertFalse(new TypedValue<>(Real.getType(), "3.0").isValidAssignmentFor(target));
        TypedVariable<UnionType> unionTarget = new TypedVariable<>(new UnionType(
                List.of(target.getType(), recipe.lang.types.Boolean.getType())), "choice");
        assertTrue(new TypedValue<>(Real.getType(), "2.0").isValidAssignmentFor(unionTarget));
        assertFalse(new TypedValue<>(Real.getType(), "3.0").isValidAssignmentFor(unionTarget));
    }

    @Test
    public void booleanPredicatesUseTheSameTypeRule() {
        Predicate predicate = new Predicate("exists", Condition.getTrue());
        assertTrue(predicate.isValidAssignmentFor(
                new TypedVariable<>(recipe.lang.types.Boolean.getType(), "flag")));
        assertFalse(predicate.isValidAssignmentFor(new TypedVariable<>(Real.getType(), "number")));
    }
}
