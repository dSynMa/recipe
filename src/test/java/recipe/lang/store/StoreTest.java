package recipe.lang.store;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StoreTest {
    Store emptyStore;
    Store store;
    Attribute attribute;

    @Before
    public void setUp() {
        attribute = new Attribute<>("v", String.class);

        Map<String, Object> data = new HashMap<>();
        data.put("v", "val");
        Map<String, Attribute<?>> attributes = new HashMap<>();
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

        Attribute attribute2 = new Attribute<>("v", Integer.class);
        assertFalse(store.safeAddAttribute(attribute2));
    }

    @Test
    public void getValue() {
        assertTrue(store.getValue("v").equals("val"));
        assertFalse(store.getValue("v").equals("vgg"));
    }

    @Test
    public void setValue() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", "bbbb");
        assertTrue(store.getValue("v").equals("bbbb"));
    }

    @Test(expected=AttributeTypeException.class)
    public void setValueAttributeTypeException() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", 6);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void setValueAttributeNotInStoreException() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("vv", "bbbb");
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