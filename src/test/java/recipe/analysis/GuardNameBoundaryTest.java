package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class GuardNameBoundaryTest {
    private String model(boolean reverse) throws Exception {
        String shortGuard = "guard g(a: boolean) := a;\n";
        String longGuard = "guard good(a: boolean) := !a;\n";
        return source("supplier-locality").replace("agent Sender",
            (reverse ? longGuard + shortGuard : shortGuard + longGuard) + "agent Sender");
    }

    @Test
    public void prefixNamesWorkInEitherDeclarationOrderAndWithCallWhitespace() throws Exception {
        for (boolean reverse : new boolean[] {false, true}) {
            for (String expression : new String[] {"good(sender-sent)", "good (sender-sent)", "!g(sender-sent)"}) {
                String output = ToNuXmv.transform(parse(model(reverse) + "\nSPEC " + expression + ";\n"));
                assertValid(output);
                assertCtl(output, output.substring(output.indexOf("LTLSPEC ") + 8).trim(), true);
            }
        }
    }

    @Test
    public void processGuardsAlsoChooseTheCompleteName() throws Exception {
        String output = ToNuXmv.transform(parse(model(false).replace("send: <TRUE>", "send: <good(sent)>")));
        assertCtl(output, "EX sender-sent", true);
        String blocked = ToNuXmv.transform(parse(model(false).replace("send: <TRUE>", "send: <g(sent)>")));
        assertCtl(blocked, "EX sender-sent", false);
    }

    @Test
    public void unknownLongerNameCannotBeAcceptedAsAKnownPrefix() throws Exception {
        parse(model(false));
        recipe.lang.utils.TypingContext context = new recipe.lang.utils.TypingContext();
        for (String name : new String[] {"g", "good"})
            context.set(name, recipe.lang.types.Guard.getDefinition(name).getType());
        assertTrue(context.guardNameParser().parse("goodbye(TRUE)").isFailure());
    }
}
