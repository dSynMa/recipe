package recipe.lang.store;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberValue;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.expressions.strings.StringVariable;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StoreTest {
    Store emptyStore;
    Store store;
    TypedVariable attribute;

    @Before
    public void setUp() throws AttributeTypeException, AttributeNotInStoreException {
        attribute = new StringVariable( "v");

        Map<String, Expression> data = new HashMap<>();
        data.put("v", new StringValue("val"));
        Map<String, TypedVariable> attributes = new HashMap<>();
        attributes.put("v", attribute);
        store = new Store(data, attributes);
        emptyStore = new Store();
    }

    @Test
    public void getAttributes() {
        assertTrue(store.getAttributes().size() == 1);
        assertTrue(emptyStore.getAttributes().size() == 0);
    }

    @Test
    public void getAttribute() throws AttributeNotInStoreException {
        assertTrue(store.getAttribute("v").equals(attribute));
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void getAttributeException() throws AttributeNotInStoreException {
        store.getAttribute("vy");
    }

    @Test
    public void safeAddAttribute() {
        assertTrue(emptyStore.safeAddAttribute(attribute));

        TypedVariable attribute2 = new StringVariable("v");
        assertFalse(store.safeAddAttribute(attribute2));
    }

    @Test
    public void getValue() {
        assertTrue(store.getValue("v").equals("val"));
        assertFalse(store.getValue("v").equals("vgg"));
    }

    @Test
    public void setValue() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", new StringValue("bbbb"));
        assertTrue(store.getValue("v").equals(new StringValue("bbbb")));
    }

    @Test(expected=AttributeTypeException.class)
    public void setValueAttributeTypeException() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", new NumberValue(6));
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void setValueAttributeNotInStoreException() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("vv", new StringValue("bbbb"));
    }

    @Test
    public void testToString() {
    }

    @Test
    public void satisfy() {
    }

    @Test
    public void waitUntil() {
    }

    @Test
    public void update() throws AttributeTypeException {
        emptyStore.update(store);
        assertTrue(store.equals(emptyStore));
    }
}