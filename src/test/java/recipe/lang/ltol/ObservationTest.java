package recipe.lang.ltol;

import org.junit.BeforeClass;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Real;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.TypeCreationException;

import java.util.ArrayList;
import java.util.List;


public class ObservationTest {
    static List<String> msgs;
    static Enum msgEnum;

    @BeforeClass
    public static void init() throws TypeCreationException {
        Enum.clear();
        msgs = new ArrayList<>();
        msgs.add("A");
        msgs.add("B");

        msgEnum = new Enum("msgvals", msgs);
    }
    @Test
    public void parser() throws Exception {
        TypingContext context = new TypingContext();

        context.set("cond", Boolean.getType());
        context.set("b", Real.getType());

        context.set("MSG", msgEnum);

        Result r;
        Parser parser = Condition.parser(context).end();
        r = parser.parse("false");
        assert r.isSuccess();
        r = parser.parse("MSG == A");
        assert r.isSuccess();
        r = parser.parse("MSG=A");
        assert r.isSuccess();
        r = parser.parse("MSG=A & MSG=B");
        assert r.isSuccess();
        r = parser.parse("(MSG=A) & (MSG=B)");
        assert r.isSuccess();
    }
}