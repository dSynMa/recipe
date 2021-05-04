package recipe.lang.utils;

import org.junit.BeforeClass;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.Config;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.types.Boolean;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.types.Enum;
import recipe.lang.types.Real;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ParsingTest {
    static List<String> channels;
    static Enum channelEnum;

    @BeforeClass
    public static void init() throws TypeCreationException {
        Enum.clear();
        channels = new ArrayList<>();
        channels.add("A");
        channels.add("*");

        channelEnum = new Enum(Config.channelLabel, channels);
    }
    @Test
    public void disjunctiveWordParser() {
    }

    @Test
    public void testDisjunctiveWordParser() {
    }

    @Test
    public void expressionParser() {
    }

    @Test
    public void variableParser() {
    }

    @Test
    public void assignmentListParser() {
    }

    @Test
    public void typedVariableList() {
    }

    @Test
    public void guardDefinitionList1() {
        String script = "guard g(a : int) := a == 5";

        Parser parser = Parsing.guardDefinitionList().end();
        Result r = parser.parse(script);
        assert r.isSuccess();
    }

    @Test
    public void testDisjunctiveWordParser1() {
    }

    @Test
    public void testDisjunctiveWordParser2() {
    }

    @Test
    public void testExpressionParser() {
    }

    @Test
    public void testVariableParser() {
    }

    @Test
    public void testAssignmentListParser() {
    }

    @Test
    public void testTypedVariableList() {
        String script = "c : channel, k : bool, kk : Boolean, K : Bool, KK : boolean, b : int, c : Int, d : Integer, dd : integer";

        Parser parser = Parsing.typedVariableList().end();

        Result r = parser.parse(script);
        assert r.isSuccess();
    }

    @Test
    public void typedAssignmentList0() throws TypeCreationException {
        String script = "c : channel := A";

        TypingContext channelContext = new TypingContext();

        channelContext.set("c", channelEnum);

        Parser parser = Parsing.typedAssignmentList(channelContext).end();

        Result r = parser.parse(script);
        assert r.isSuccess();
    }

    @Test
    public void typedAssignmentList1() throws TypeCreationException {
        String script =
                "b : int := 0\n" +
                        "\t\tchVar : channel := A";

        TypingContext channelContext = new TypingContext();

        Parser parser = Parsing.typedAssignmentList(channelContext).end();

        Result r = parser.parse(script);
        assert r.isSuccess();
    }

    @Test
    public void typedAssignmentList2() throws TypeCreationException {
        String script =
                "b : int := 0\n" +
                        "\t\tb : int := 0";

        TypingContext channelContext = new TypingContext();
        List<String> channels = new ArrayList<>();
        channels.add("A");
        channels.add("*");

        Enum channelEnum = new Enum("channels", channels);

        channelContext.set("A", channelEnum);

        Parser parser = Parsing.typedAssignmentList(channelContext).end();

        Result r = parser.parse(script);
        assert r.isSuccess();
    }

    @Test
    public void testGuardDefinitionList() {
    }

    @Test
    public void channelValues() {
    }

    @Test
    public void labelledParser() {
    }

    @Test
    public void conditionalFail() {
    }

    @Test
    public void relabellingParser() {
        String script = "\trelabel:\n" +
                "\t\tf <- 1\n" +
                "\t\tg <- b != 0";

        TypingContext localContext = new TypingContext();
        localContext.set("b", Real.getType());

        TypingContext communicativeContext = new TypingContext();
        communicativeContext.set("f", Real.getType());
        communicativeContext.set("g", Boolean.getType());

        Parser parser = Parsing.relabellingParser(localContext, communicativeContext).end();
        Result r = parser.parse(script);
        assert r.isSuccess();
    }

    @Test
    public void receiveGuardParser1() throws Exception {
        String script = "receive-guard:\n" +
        "true";

        TypingContext localContext = new TypingContext();
        localContext.set("b", Real.getType());
        localContext.set("c", Real.getType());

        TypingContext channelContext = new TypingContext();

        Parser parser = Parsing.receiveGuardParser(localContext, channelContext).end();
        Result r = parser.parse(script);
        System.out.println(r.getPosition() + " " + r.getMessage());
        assert r.isSuccess();
    }

    @Test
    public void receiveGuardParser2() throws Exception {
        String script = "receive-guard:\n" +
        "true & 1 == 0";

        TypingContext localContext = new TypingContext();
        localContext.set("b", Real.getType());
        localContext.set("c", Real.getType());

        TypingContext channelContext = new TypingContext();

        Parser parser = Parsing.receiveGuardParser(localContext, channelContext).end();
        Result r = parser.parse(script);
        System.out.println(r.getPosition() + " " + r.getMessage());
        assert r.isSuccess();
    }

    @Test
    public void receiveGuardParser3() throws Exception {
        String script = "receive-guard:\n (channel == A)";

        TypingContext localContext = new TypingContext();
        localContext.set("b", Real.getType());
        localContext.set("c", Real.getType());

        TypingContext channelContext = new TypingContext();

        channelContext.set("channel", channelEnum);

        Parser parser = Parsing.receiveGuardParser(localContext, channelContext).end();
        Result r = parser.parse(script);
        System.out.println(r.getPosition() + " " + r.getMessage());
        assert r.isSuccess();
    }

    @Test
    public void receiveGuardParser4() throws Exception {
        String script = "receive-guard:\n" +
                    "\t\t((b < 5) & channel == A)";

        TypingContext localContext = new TypingContext();
        localContext.set("b", Real.getType());
        localContext.set("c", Real.getType());

        TypingContext channelContext = new TypingContext();
        channelContext.set("channel", channelEnum);
        channelContext.set("chVar", channelEnum);

        Parser parser = Parsing.receiveGuardParser(localContext, channelContext).end();
        Result r = parser.parse(script);
        System.out.println(r.getPosition() + " " + r.getMessage());
        System.out.println(script.substring(r.getPosition()));
        assert r.isSuccess();
    }
    @Test
    public void receiveGuardParser5() throws Exception {
        String script = "receive-guard:\n" +
                    "\t\t((b < 5) & channel == A))";

        TypingContext localContext = new TypingContext();
        localContext.set("b", Real.getType());
        localContext.set("c", Real.getType());

        TypingContext channelContext = new TypingContext();
        channelContext.set("channel", channelEnum);
        channelContext.set("chVar", channelEnum);

        Parser parser = Parsing.receiveGuardParser(localContext, channelContext).end();
        Result r = parser.parse(script);
        System.out.println(r.getPosition() + " " + r.getMessage());
        System.out.println(script.substring(r.getPosition()));
        assert !r.isSuccess();
    }
}