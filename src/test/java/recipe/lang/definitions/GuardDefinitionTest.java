package recipe.lang.definitions;

import org.junit.Test;
import org.petitparser.parser.Parser;
import recipe.lang.types.Guard;
import recipe.lang.utils.TypingContext;

import javax.xml.transform.Result;

import static org.junit.Assert.*;

public class GuardDefinitionTest {

    @Test
    public void parser() {
        Guard.clear();
        Parser parser = GuardDefinition.parser(new TypingContext());

        String script = "guard g(s : integer) := s > 5;";
        org.petitparser.context.Result r = parser.parse(script);
        if(r.isFailure()) System.out.println(r.getMessage() + "\n" + script.substring(r.getPosition()));
        assert r.isSuccess();
    }

    @Test
    public void testParser() {
    }
}