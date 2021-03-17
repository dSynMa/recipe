package recipe.lang.expressions;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Attribute;
import recipe.lang.store.Store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class StringVariableTest {
    Store emptyStore;
    Store store;
    Store store1;
    Attribute attribute;
    String attributeVal;

    @Before
    public void setUp() {
        attribute = new Attribute<>("v", String.class);
        attributeVal = "val";

        emptyStore = new Store();

        Map<String, Object> data = new HashMap<>();
        data.put("v", attributeVal);
        Map<String, Attribute<?>> attributes = new HashMap<>();
        attributes.put("v", attribute);
        store = new Store(data, attributes);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("v", 6);
        Map<String, Attribute<?>> attributes1 = new HashMap<>();
        Attribute attribute1 = new Attribute<>("v", Integer.class);

        attributes1.put("v", attribute1);
        store1 = new Store(data1, attributes1);

    }

    @Test
    public void correctValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", "example");
        StringVariable stringVariable = new StringVariable("v");
        StringValue value = stringVariable.valueIn(store);
        assertTrue(value.value.equals("example"));
    }

    @Test(expected=AttributeTypeException.class)
    public void valueInWrongTypeValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", 8);
        StringVariable stringVariable = new StringVariable("v");
        stringVariable.valueIn(store);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void valueInNotInStoreExceptionThrown() throws AttributeTypeException, AttributeNotInStoreException {
        StringVariable stringVariable = new StringVariable("vv");
        stringVariable.valueIn(store);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void varNotInCVAndNotAttribute() throws AttributeTypeException, AttributeNotInStoreException {
        StringVariable stringVariable = new StringVariable("x");
        Set<String> CV = new HashSet<>();

        stringVariable.close(store, CV);
    }

    @Test
    public void varNotInCVAndAttribute() throws AttributeTypeException, AttributeNotInStoreException {
        StringVariable stringVariable = new StringVariable("v");
        Set<String> CV = new HashSet<>();

        Expression closure = stringVariable.close(store, CV);
        StringValue val = new StringValue(attributeVal);

        assertEquals(val, closure);
    }

    @Test(expected=AttributeTypeException.class)
    public void closeNotInCVAttributeTypeException() throws AttributeTypeException, AttributeNotInStoreException {
        StringVariable stringVariable = new StringVariable("v");
        Set<String> CV = new HashSet<>();

        stringVariable.close(store1, CV);
    }
}