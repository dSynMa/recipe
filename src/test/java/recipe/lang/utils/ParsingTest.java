package recipe.lang.utils;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;

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
}