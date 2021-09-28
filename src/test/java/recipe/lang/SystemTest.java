package recipe.lang;

import org.junit.BeforeClass;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.types.Enum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class SystemTest {

    @BeforeClass
    public static void init() throws TypeCreationException {
        Enum.clear();
    }

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
    public void parser() throws IOException {
        String script = String.join("\n", Files.readAllLines(Paths.get("./example-current-syntax.txt")));

        Parser system = System.parser().end();
        Result r = system.parse(script);
        java.lang.System.out.println(r.getMessage());
        java.lang.System.out.println(script.substring(r.getPosition()));
        assert r.isSuccess();
    }
}