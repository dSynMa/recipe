package recipe.lang.expressions.predicate;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
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
        Guard guard = new Guard("h", new TypedVariable[0]);
        typingContext.set("h", guard);

        String text = "h()";

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