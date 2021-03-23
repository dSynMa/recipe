package recipe.lang.expressions.arithmetic;

import org.junit.Test;
import org.petitparser.parser.Parser;

import static org.junit.Assert.*;

public class NumberValueTest {

    @Test
    public void valueIn() {
    }

    @Test
    public void close() {
    }

    @Test
    public void getValue() {
    }

    @Test
    public void parserInteger() {
        Parser parser = NumberValue.parser();
        Number n = parser.parse("6").get();
        assertTrue(n.equals(6));
    }

    @Test
    public void parserDecimal() {
        Parser parser = NumberValue.parser();
        Number n = parser.parse("9.9999").get();
        assertTrue(n.equals(9.9999));
    }
}