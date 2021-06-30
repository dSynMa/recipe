package recipe.analysis;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.System;
import recipe.lang.exception.RelabellingTypeException;

import static org.junit.Assert.*;

public class ToNuXmvTest {

    @Test
    public void transform() throws Exception {
        String script = "channels: ch1,ch2,ch3\n" +
//                "message-label: team, form, assemble\n" +
                "message-structure: d1 : 0..8, d2 : bool\n" +
                "communication-variables: f : int, g : bool\n\n" +
                "agent first\n" +
                "\tlocal:\n" +
                "\t\tb : Int := 0\n" +
                "\t\tc : Int := 0\n" +
                "\trelabel:\n" +
                "\t\tf <- c\n" +
                "\t\tg <- b != 0\n" +
                "\treceive-guard: channel != ch1\n" +
                "\trepeat: (<b == 0> ch3! (c == f) (d1 := b + 1, d2 := false)[b := b + 1])\n\n" +
                "agent second\n" +
                "\tlocal:\n" +
                "\t\ta : Int := 0\n" +
                "\trelabel:\n" +
                "\t\tf <- a\n" +
                "\t\tg <- a == 0\n" +
                "\treceive-guard:\n" +
                "\t\t(a < 5 & channel == ch3) | (a > 3 & channel == ch2)\n" +
                "\trepeat:(<true> ch1! (true) (d1 := a; d2 := true) [a:= a + 1]) + (<a > 3> ch1? [a := d1])";

        Parser system = System.parser().end();
        Result r = system.parse(script);
        System s = r.get();
        try {
            String transform = ToNuXmv.transform(s);
            java.lang.System.out.println(transform);
        } catch (RelabellingTypeException e) {
            e.printStackTrace();
        }
        assert r.isSuccess();
    }
}