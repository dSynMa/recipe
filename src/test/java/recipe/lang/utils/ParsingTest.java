package recipe.lang.utils;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.expressions.predicate.IsEqualTo;

import static org.junit.Assert.*;

public class ParsingTest {

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
    public void guardDefinitionList() {
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
    }

    @Test
    public void typedAssignmentList() {
        String script =
                "b : int := 0\n" +
                        "\t\tchVar : channel := A";

        TypingContext channelContext = new TypingContext();
        channelContext.set("A", new ChannelValue("A"));

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
        localContext.set("b", new NumberVariable("b"));

        TypingContext communicativeContext = new TypingContext();
        communicativeContext.set("f", new NumberVariable("f"));
        communicativeContext.set("g", new BooleanVariable("g"));

        Parser parser = Parsing.relabellingParser(localContext, communicativeContext).end();
        Result r = parser.parse(script);
        assert r.isSuccess();
    }

    @Test
    public void receiveGuardParser1() {
        String script = "receive-guard:\n" +
        "true";

        TypingContext localContext = new TypingContext();
        localContext.set("b", new NumberVariable("b"));
        localContext.set("c", new NumberVariable("c"));

        TypingContext channelContext = new TypingContext();
        channelContext.set("A", new ChannelValue("A"));
        channelContext.set("*", new ChannelValue("*"));

        Parser parser = Parsing.receiveGuardParser(localContext, channelContext).end();
        Result r = parser.parse(script);
        System.out.println(r.getPosition() + " " + r.getMessage());
        assert r.isSuccess();
    }

    @Test
    public void receiveGuardParser2() {
        String script = "receive-guard:\n" +
        "true & 1 == 0";

        TypingContext localContext = new TypingContext();
        localContext.set("b", new NumberVariable("b"));
        localContext.set("c", new NumberVariable("c"));

        TypingContext channelContext = new TypingContext();
        channelContext.set("A", new ChannelValue("A"));
        channelContext.set("*", new ChannelValue("*"));

        Parser parser = Parsing.receiveGuardParser(localContext, channelContext).end();
        Result r = parser.parse(script);
        System.out.println(r.getPosition() + " " + r.getMessage());
        assert r.isSuccess();
    }

    @Test
    public void receiveGuardParser3() {
        String script = "receive-guard: channel == A";

        TypingContext localContext = new TypingContext();
        localContext.set("b", new NumberVariable("b"));
        localContext.set("c", new NumberVariable("c"));

        TypingContext channelContext = new TypingContext();
        channelContext.set("A", new ChannelValue("A"));
        channelContext.set("*", new ChannelValue("*"));
        channelContext.set("channel", new ChannelVariable("channel"));

        Parser parser = Parsing.receiveGuardParser(localContext, channelContext).end();
        Result r = parser.parse(script);
        System.out.println(r.getPosition() + " " + r.getMessage());
        assert r.isSuccess();
    }
}