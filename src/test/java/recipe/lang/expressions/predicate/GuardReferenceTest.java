package recipe.lang.expressions.predicate;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.definitions.GuardDefinition;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.types.Guard;
import recipe.lang.types.Integer;
import recipe.lang.utils.TypingContext;

import static org.junit.Assert.*;

public class GuardReferenceTest {

    @Test
    public void parser1() throws Exception {
        Guard.clear();
        TypingContext typingContext = new TypingContext();
        Result result = GuardDefinition.parser(typingContext).end().parse("guard h(x: bool) := TRUE;");
        GuardDefinition o = result.get();
        Guard.setDefinition("h", o);

        Guard guard = o.getType();
        typingContext.set("h", guard);

        String text = "h(TRUE)";

        Parser parser = GuardReference.parser(typingContext);
        Result r = parser.parse(text);

        assert r.isSuccess();
    }

    @Test
    public void parser3() throws Exception {
        Guard.clear();
        TypingContext typingContext = new TypingContext();
        Result result = GuardDefinition.parser(typingContext).end().parse("guard g2(r : int, s : int) := (r==s);");
        GuardDefinition o = result.get();
        Guard.setDefinition("g2", o);

        Guard guard = o.getType();
        typingContext.set("g2", guard);

        String text = "g2(1,2)";

        Parser parser = GuardReference.parser(typingContext);
        Result r = parser.parse(text);

        assert r.isSuccess();
    }

    @Test
    public void parser2() throws Exception {
        Guard.clear();
        TypingContext typingContext = new TypingContext();
        TypedVariable[] typedVariables = new TypedVariable[1];
        typedVariables[0] = new TypedVariable(Integer.getType(), "v");

        Result result = GuardDefinition.parser(typingContext).end().parse("guard z(r : int) := r > 5;");
        GuardDefinition o = result.get();
        Guard.setDefinition("z", o);

        Guard guard = new Guard("z", typedVariables);
        typingContext.set("z", guard);

        String text = "z(6)";

        Parser parser = GuardReference.parser(typingContext);
        Result r = parser.parse(text);

        assert r.isSuccess();
    }

    @Test
    public void testToString() throws Exception {
        Guard.clear();
        Parser parser = GuardDefinition.parser(new TypingContext());
        TypingContext typingContext1 = new TypingContext();
        typingContext1.set("s", Integer.getType());
        Result rr = Condition.parser(typingContext1).end().parse("s > 5");
        if(rr.isFailure()) System.out.println(rr.getMessage() + "\n" + "s > 5".substring(rr.getPosition()));
        assert rr.isSuccess();


        String script = "guard g(s : integer) := s > 5;";
        org.petitparser.context.Result r = parser.parse(script);
        if(r.isFailure()) System.out.println(r.getMessage() + "\n" + script.substring(r.getPosition()));
        assert r.isSuccess();
        GuardDefinition guardDefinition = r.get();
        Guard.setDefinition(guardDefinition.getName(), guardDefinition);

        TypingContext typingContext = new TypingContext();
        TypedVariable[] typedVariables = new TypedVariable[1];
        typedVariables[0] = new TypedVariable(Integer.getType(), "v");

        Guard guard = guardDefinition.getType();
        typingContext.set("g", guard);

        String text = "g(6)";

        Parser parserr = Condition.parser(typingContext);
        Result rrr = parserr.end().parse(text);
        String toString = ((GuardReference) rrr.get()).toString();
        assert toString.equals("g(6)");
        GuardReference.resolve = true;
        toString = ((GuardReference) rrr.get()).toString();
        assert toString.equals("6 > 5");
        GuardReference.resolve = true;
    }
}