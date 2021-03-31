package recipe.lang;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;

import static org.junit.Assert.*;

public class SystemTest {

    @Test
    public void getChannels() {
    }

    @Test
    public void getMessageStructure() {
    }

    @Test
    public void getCommunicationVariables() {
    }

    @Test
    public void getAgents() {
    }

    @Test
    public void parser() {
        String script = "channels: A,B,C\n" +
                "message-structure: d1 : int, d2 : bool\n" +
                "communication-variables: f : int, g : bool\n\n" +
                "guard g(a : int) := a == 5\n\n" +
                "agent B\n" +
                "\tlocal:\n" +
                "\t\tb : Int := 0\n" +
                "\t\tc : Int := 0\n" +
                "\trelabel:\n" +
                "\t\tf <- c\n" +
                "\t\tg <- b != 0\n" +
                "\treceive-guard: true\n" +
                "\trepeat: (<b == 0> C! (c == f) (d1 := b + 1, d2 := false)[b := b + 1])\n\n" +
                "agent A\n" +
                "\tlocal:\n" +
                "\t\ta : Int := 0\n" +
                "\trelabel:\n" +
                "\t\tf <- a\n" +
                "\t\tg <- a == 0\n" +
                "\treceive-guard:\n" +
                "\t\t(a < 5 & channel == C) | (a > 3 & channel == *)\n" +
                "\trepeat: B + (<a > 3> C? [a := d1])";

        Parser system = System.parser();
        Result r = system.parse(script);
        java.lang.System.out.println(script.substring(r.getPosition()));
        assert r.isSuccess();
    }
}