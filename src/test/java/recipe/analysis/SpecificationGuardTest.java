package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SpecificationGuardTest {
    private String model() throws Exception {
        return source("supplier-locality")
            .replace("agent Sender", "guard allowed(a: boolean) := !a;\nagent Sender");
    }

    @Test
    public void namedGuardsWorkInStateAndQuantifiedSpecifications() throws Exception {
        for (String spec : new String[] {"allowed(sender-sent)", "/\\ x: Sender . (allowed(x-sent))"}) {
            String output = ToNuXmv.transform(parse(model() + "\nSPEC " + spec + ";\n"));
            assertValid(output);
            assertCtl(output, output.substring(output.indexOf("LTLSPEC ") + 8).trim(), true);
        }
    }

    @Test
    public void parsedSpecificationKeepsItsGuardAfterAnotherModelIsParsed() throws Exception {
        recipe.lang.System first = parse(model() + "\nSPEC allowed(sender-sent);\n");
        parse(model().replace(":= !a", ":= a"));
        String output = ToNuXmv.transform(first);
        assertValid(output);
        assertCtl(output, output.substring(output.indexOf("LTLSPEC ") + 8).trim(), true);
    }

    @Test
    public void guardsCanReceiveMessageFieldsInObservations() throws Exception {
        String model = source("observation-forall-constant")
            .replace("agent Sender", "guard allowed(a: boolean) := !a;\nagent Sender")
            .replace("forall(TRUE)", "allowed(value)");
        assertCtl(ToNuXmv.transform(parse(model)), "EX obs0", false);
        assertCtl(ToNuXmv.transform(parse(model.replace(":= !a", ":= a"))), "EX obs0", true);
    }

    @Test
    public void guardsOutsideTheTypingContextDoNotProduceNullReferences() throws Exception {
        parse(model());
        org.petitparser.context.Result result = recipe.lang.expressions.predicate.Condition
            .parser(new recipe.lang.utils.TypingContext()).end().parse("allowed(TRUE)");
        assertTrue(result.toString(), result.isFailure());
    }
}
