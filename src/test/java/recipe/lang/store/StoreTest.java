package recipe.lang.store;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Boolean;
import recipe.lang.types.Integer;
import recipe.lang.types.Real;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StoreTest {
    Store emptyStore;
    Store store;
    TypedVariable attribute;

    @Before
    public void setUp() throws AttributeTypeException, AttributeNotInStoreException, MismatchingTypeException {
        attribute = new TypedVariable(Integer.getType(),"v");

        Map<String, TypedValue> data = new HashMap<>();
        data.put("v", new TypedValue(Integer.getType(), "7"));
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

        TypedVariable attribute2 = new TypedVariable(Real.getType(), "v");
        assertFalse(store.safeAddAttribute(attribute2));
    }

    @Test
    public void getValue() {
        assertTrue(store.getValue("v").getValue().toString().equals("7"));
        assertFalse(store.getValue("v").equals("vgg"));
    }

    @Test
    public void setValue() throws AttributeTypeException, AttributeNotInStoreException, MismatchingTypeException {
        store.setValue("v", new TypedValue(Integer.getType(), "888"));
        assertTrue(store.getValue("v").equals(new TypedValue(Integer.getType(), "888")));
    }

    @Test(expected=AttributeTypeException.class)
    public void setValueAttributeTypeException() throws AttributeTypeException, AttributeNotInStoreException, MismatchingTypeException {
        store.setValue("v", new TypedValue(Boolean.getType(), "true"));
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void setValueAttributeNotInStoreException() throws AttributeTypeException, AttributeNotInStoreException, MismatchingTypeException {
        store.setValue("vv", new TypedValue(Integer.getType(), "888"));
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