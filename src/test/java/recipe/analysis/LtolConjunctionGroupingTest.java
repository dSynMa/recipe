package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.ltol.And;
import recipe.lang.ltol.Atom;
import recipe.lang.ltol.LTOL;
import recipe.lang.types.Boolean;

import static recipe.analysis.TranslationTestModels.assertLtl;

public class LtolConjunctionGroupingTest {
    @Test
    public void conjunctionGroupsDisjunctiveAtomsOnEitherSide() throws Exception {
        TypedVariable<Boolean> a = new TypedVariable<>(Boolean.getType(), "a");
        TypedVariable<Boolean> b = new TypedVariable<>(Boolean.getType(), "b");
        TypedVariable<Boolean> c = new TypedVariable<>(Boolean.getType(), "c");
        LTOL choice = new Atom(new recipe.lang.expressions.predicate.Or(a, b));
        LTOL required = new Atom(c);
        String model = "MODULE main\nVAR a : boolean; b : boolean; c : boolean;\n";
        for (LTOL conjunction : new LTOL[] {new And(choice, required), new And(required, choice)}) {
            for (String rendered : new String[] {conjunction.toString(), conjunction.toNuXmv()}) {
                assertLtl(model, "G ((" + rendered + ") <-> ((a | b) & c))", true);
            }
        }
    }
    @Test
    public void disjunctionGroupsImplicativeAtomsOnEitherSide() throws Exception {
        TypedVariable<Boolean> a = new TypedVariable<>(Boolean.getType(), "a");
        TypedVariable<Boolean> b = new TypedVariable<>(Boolean.getType(), "b");
        TypedVariable<Boolean> c = new TypedVariable<>(Boolean.getType(), "c");
        LTOL implication = new Atom(new recipe.lang.expressions.predicate.Implies(a, b));
        LTOL alternative = new Atom(c);
        String model = "MODULE main\nVAR a : boolean; b : boolean; c : boolean;\n";
        for (LTOL formula : new LTOL[] {
                new recipe.lang.ltol.Or(implication, alternative),
                new recipe.lang.ltol.Or(alternative, implication)}) {
            for (String rendered : new String[] {formula.toString(), formula.toNuXmv()}) {
                assertLtl(model, "G ((" + rendered + ") <-> ((a -> b) | c))", true);
            }
        }
    }
    @Test
    public void untilAndEquivalenceGroupCompoundAtoms() throws Exception {
        TypedVariable<Boolean> a = new TypedVariable<>(Boolean.getType(), "a");
        TypedVariable<Boolean> b = new TypedVariable<>(Boolean.getType(), "b");
        TypedVariable<Boolean> c = new TypedVariable<>(Boolean.getType(), "c");
        LTOL choice = new Atom(new recipe.lang.expressions.predicate.Or(a, b));
        LTOL other = new Atom(c);
        LTOL[] formulas = {
                new recipe.lang.ltol.Until(choice, other),
                new recipe.lang.ltol.Until(other, choice),
                new recipe.lang.ltol.Iff(choice, other),
                new recipe.lang.ltol.Iff(other, choice)
        };
        String[] expected = {"((a | b) U c)", "(c U (a | b))",
                "((a | b) <-> c)", "(c <-> (a | b))"};
        String model = "MODULE main\nVAR a : boolean; b : boolean; c : boolean;\n";
        for (int i = 0; i < formulas.length; i++) {
            for (String rendered : new String[] {formulas[i].toString(), formulas[i].toNuXmv()}) {
                assertLtl(model, "G ((" + rendered + ") <-> " + expected[i] + ")", true);
            }
        }
    }

}
