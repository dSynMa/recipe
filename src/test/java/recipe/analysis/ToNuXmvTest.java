package recipe.analysis;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.System;
import recipe.lang.exception.RelabellingTypeException;

import static org.junit.Assert.*;

public class ToNuXmvTest {

    @Test
    public void transform() {
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
                "\trepeat: (<a > 3> C? [a := d1])";

        Parser system = System.parser().end();
        Result r = system.parse(script);
        System s = r.get();
        try {
            String transform = ToNuXmv.transform(s);
        } catch (RelabellingTypeException e) {
            e.printStackTrace();
        }
        java.lang.System.out.println(script.substring(r.getPosition()));
        assert r.isSuccess();
    }
}