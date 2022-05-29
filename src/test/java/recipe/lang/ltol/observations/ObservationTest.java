package recipe.lang.ltol.observations;

import org.junit.Before;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.System;
import recipe.lang.ltol.LTOL;
import recipe.lang.types.Boolean;
import recipe.lang.types.Integer;
import recipe.lang.utils.TypingContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ObservationTest {

    @Before
    public void setUp() throws IOException {
        String script = String.join("\n", Files.readAllLines(Paths.get("./bigger-example.txt")));

        Parser system = System.parser().end();
        Result r = system.parse(script);
        System s = r.get();
    }

    @Test
    public void parser() throws Exception {
        TypingContext commonVars = new TypingContext();
        commonVars.set("asgn", Boolean.getType());
        TypingContext messageVars = new TypingContext();

        Parser obsParser = Observation.parser(commonVars, messageVars);
        try {
            String spec = "asgn";
            Result parse = obsParser.parse(spec);
            assert(parse.isSuccess());
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
        assert(true);
    }

    @Test
    public void parserForall() throws Exception {
        TypingContext commonVars = new TypingContext();
        commonVars.set("asgn", Boolean.getType());
        TypingContext messageVars = new TypingContext();

        Parser obsParser = Observation.parser(commonVars, messageVars);
        try {
            String spec = "forall(asgn)";
            Result parse = obsParser.parse(spec);
            assert(parse.isSuccess());
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
        assert(true);
    }

    @Test
    public void parserExists() throws Exception {
        TypingContext commonVars = new TypingContext();
        commonVars.set("asgn", Boolean.getType());
        TypingContext messageVars = new TypingContext();

        Parser obsParser = Observation.parser(commonVars, messageVars);
        try {
            String spec = "exists(asgn)";
            Result parse = obsParser.parse(spec);
            assert(parse.isSuccess());
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
        assert(true);
    }
}