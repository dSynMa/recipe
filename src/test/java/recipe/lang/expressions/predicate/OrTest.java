package recipe.lang.expressions.predicate;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.store.Store;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class OrTest {
    BooleanVariable booleanVariable;
    BooleanValue booleanValue;
    Or or;
    Store store;

    @Before
    public void setUp() throws Exception {
        booleanVariable = new BooleanVariable("v");
        booleanValue = new BooleanValue(false);

        or = new Or(booleanValue, booleanVariable);

        Map<String, Object> data = new HashMap<>();
        data.put("v", booleanValue);
    }

    @Test
    public void testEquals() {
    }

    @Test
    public void testHashCode() {
    }

    @Test
    public void testToString() {
    }

    @Test
    public void getLhs() {
        assertEquals(or.getLhs(), booleanValue);
    }

    @Test
    public void getRhs() {
        assertEquals(or.getLhs(), booleanVariable);
    }

    @Test
    public void valueIn() {

    }

    @Test
    public void close() {
    }
}