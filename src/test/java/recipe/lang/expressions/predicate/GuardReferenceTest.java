package recipe.lang.expressions.predicate;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.definitions.GuardDefinition;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Guard;
import recipe.lang.types.Integer;
import recipe.lang.utils.TypingContext;

import static org.junit.Assert.*;

public class GuardReferenceTest {

    @Test
    public void parser1() throws Exception {
        TypingContext typingContext = new TypingContext();
        Result result = GuardDefinition.parser(typingContext).end().parse("guard f(x: bool) := TRUE;");
        GuardDefinition o = result.get();

        Guard guard = o.getType();
        typingContext.set("h", guard);

        String text = "h(TRUE)";

        Parser parser = GuardReference.parser(typingContext);
        Result r = parser.parse(text);

        assert r.isSuccess();
    }

    @Test
    public void parser3() throws Exception {
        TypingContext typingContext = new TypingContext();
        Result result = GuardDefinition.parser(typingContext).end().parse("guard g2(r : int, s : int) := (r==s);");
        GuardDefinition o = result.get();

        Guard guard = o.getType();
        typingContext.set("g2", guard);

        String text = "g2(1,2)";

        Parser parser = GuardReference.parser(typingContext);
        Result r = parser.parse(text);

        assert r.isSuccess();
    }

    @Test
    public void parser2() throws Exception {
        TypingContext typingContext = new TypingContext();
        TypedVariable[] typedVariables = new TypedVariable[1];
        typedVariables[0] = new TypedVariable(Integer.getType(), "v");

        Guard guard = new Guard("h", typedVariables);
        typingContext.set("h", guard);

        String text = "h(6)";

        Parser parser = GuardReference.parser(typingContext);
        Result r = parser.parse(text);

        assert r.isSuccess();
    }
}