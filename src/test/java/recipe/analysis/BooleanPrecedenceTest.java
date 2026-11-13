package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;
import recipe.lang.utils.TypingContext;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class BooleanPrecedenceTest {
    @Test
    public void temporalConnectivesFollowBooleanPrecedenceAndImplicationAssociativity() throws Exception {
        for (String formula : new String[] {
            "G false & G true -> G false", "G true | G false & G false",
            "G false -> G true -> G false", "G false & G true <-> G false"
        }) {
            String output = ToNuXmv.transform(parse(source("supplier-locality") + "\nSPEC " + formula + ";\n"));
            String actual = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
            assertLtl(output, "(" + actual + ") <-> (" + formula.replace("false", "FALSE").replace("true", "TRUE") + ")", true);
        }
    }

    @Test
    public void guardConnectivesFollowTheSamePrecedenceForAllValuations() throws Exception {
        TypingContext context = new TypingContext();
        for (String name : new String[] {"a", "b", "c"}) context.set(name, Boolean.getType());
        for (String formula : new String[] {"a & b -> c", "a | b & c", "a -> b -> c", "a & b <-> c"}) {
            org.petitparser.context.Result result = Condition.parser(context).end().parse(formula);
            assertTrue(result.toString(), result.isSuccess());
            Condition condition = result.get();
            assertLtl("MODULE main\nVAR a : boolean; b : boolean; c : boolean;\n",
                "G ((" + condition + ") <-> (" + formula + "))", true);
        }
    }
}
