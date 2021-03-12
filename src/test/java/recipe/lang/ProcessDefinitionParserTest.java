package recipe.lang;

import org.junit.Test;

public class ProcessDefinitionParserTest {

    @Test
    public void validationTestParse() {
        String script = "channels: a,b,c\n" +
                "message-structure: int d1, bool d2\n" +
                "communication-variables: int f, bool g\n\n" +
                "guard g(a : int) := a == 5\n\n" +
                "agent B\n" +
                "\tlocal:\n" +
                "\t\tb : Int := 0\n" +
                "\t\tc : Int := 0\n" +
                "\trelabel:\n" +
                "\t\tf <- c\n" +
                "\t\tg <- b != 0\n" +
                "\tbehaviour: (<b == 0> c! (my.f = f) (d1 := b + 1, d2 := false)[b++])\n\n" +
                "agent A\n" +
                "\tlocal:\n" +
                "\t\ta : Int := 0\n" +
                "\trelabel:\n" +
                "\t\tf <- a\n" +
                "\t\tg <- a == 0\n" +
                "\tbehaviour: B + (a == 0 ? c ....)\n\n" +
                "\tsystem: A | B";

        ProcessDefinitionParser processDefinitionParser = new ProcessDefinitionParser();

        assert(processDefinitionParser.parse(script));
    }
}