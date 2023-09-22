package recipe.lang.ltol.observations;

import org.junit.Before;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.Config;
import recipe.lang.System;
import recipe.lang.ltol.Observation;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Integer;
import recipe.lang.utils.TypingContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ObservationTest {

    @Before
    public void setUp() throws IOException {
        Config.reset();
        String script = String.join("\n", Files.readAllLines(Paths.get("./bigger-example.txt")));

        Parser system = System.parser().end();
        Result r = system.parse(script);
        // System s =
        r.get();
    }
    @Test
    public void parser() throws Exception {
        TypingContext commonVars = new TypingContext();
        commonVars.set("asgn", Boolean.getType());
        TypingContext messageVars = new TypingContext();

        Parser obsParser = Observation.parser(commonVars, messageVars, new TypingContext());
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

        Parser obsParser = Observation.parser(commonVars, messageVars, new TypingContext());
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

        Parser obsParser = Observation.parser(commonVars, messageVars, new TypingContext());
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
//    SPEC G((one-prd = 1 & one-stage = 0 & (<sender=one & channel = *>true)) -> <exists(type = 1) & exists(type = t2) & forall(type = 1 | type = 2)>true)

    @Test
    public void parserSender() throws Exception {
        TypingContext commonVars = new TypingContext();
        commonVars.set("type", Integer.getType());
        TypingContext messageVars = new TypingContext();
        TypingContext agentNames = new TypingContext();
        ArrayList<String> agent = new ArrayList<>();
        agent.add("one");
        new Enum("Agent", agent);
        Config.addAgentTypeName("Agent", null);
        agentNames.set("one", Config.getAgentType());

        Parser obsParser = Observation.parser(commonVars, messageVars, agentNames);
        try {
            String spec = "exists(type = 1) & exists(type = 2) & forall(type = 1 | type = 2)";
            Result parse = obsParser.parse(spec);
            assert(parse.isSuccess());
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
        assert(true);
    }
}