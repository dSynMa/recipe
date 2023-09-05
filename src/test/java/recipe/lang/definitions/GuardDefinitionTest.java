package recipe.lang.definitions;

import org.junit.Test;
import org.petitparser.parser.Parser;
import recipe.lang.types.Guard;
import recipe.lang.utils.TypingContext;

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
    public void parser2() {
        Guard.clear();
        Parser parser = GuardDefinition.parser(new TypingContext());

        String script = "guard g0(p : bool, p2: bool) := (p && p2);";
        org.petitparser.context.Result r = parser.parse(script);
        if(r.isFailure()) System.out.println(r.getMessage() + "\n" + script.substring(r.getPosition()));
        assert r.isSuccess();
    }

    @Test
    public void testParser() {
    }
}