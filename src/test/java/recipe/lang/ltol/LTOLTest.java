package recipe.lang.ltol;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.Config;
import recipe.lang.System;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class LTOLTest {

    //TODO tests that include values, e.g. true and false

    @Test
    public void parser() throws Exception {
        Config.reset();
        String script = String.join("\n", Files.readAllLines(Paths.get("./bigger-example.txt")));

        Parser system = System.parser().end();
        Result r = system.parse(script);
        System s = r.get();

        Parser ltolParser = LTOL.parser(s);
        try {
            String spec = "<forall(asgn)>(one-stage > 1)";
            Result parse = ltolParser.parse(spec);
            assert(parse.isSuccess());
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
        assert(true);
    }
}