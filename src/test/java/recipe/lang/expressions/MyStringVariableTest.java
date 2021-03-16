package recipe.lang.expressions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Attribute;
import recipe.lang.store.Store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class MyStringVariableTest {
    Store emptyStore;
    Store store;
    Store store1;
    Attribute attribute;

    @Before
    public void setUp() {
        attribute = new Attribute<>("v", String.class);

        emptyStore = new Store();

        Map<String, Object> data = new HashMap<>();
        data.put("v", "val");
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
        MyStringVariable myStringVariable = new MyStringVariable("v");
        StringValue value = myStringVariable.valueIn(store);
        assertTrue(value.value.equals("example"));
    }

    @Test(expected=AttributeTypeException.class)
    public void valueInWrongTypeValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", 8);
        MyStringVariable myStringVariable = new MyStringVariable("v");
        myStringVariable.valueIn(store);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void valueInNotInStoreExceptionThrown() throws AttributeTypeException, AttributeNotInStoreException {
        MyStringVariable myStringVariable = new MyStringVariable("vv");
        myStringVariable.valueIn(store);
    }

    @Test
    public void closeInCV() throws AttributeTypeException, AttributeNotInStoreException {
        MyStringVariable myStringVariable = new MyStringVariable("v");
        Set<String> CV = new HashSet<>();
        CV.add("v");

        Object closure = myStringVariable.close(store, CV);

        assertTrue(closure.getClass().equals(StringValue.class));

        assertTrue(((StringValue) myStringVariable.close(store, CV)).value.equals("val"));
    }

    @Test
    public void closeNotInCV() throws AttributeTypeException, AttributeNotInStoreException {
        MyStringVariable myStringVariable = new MyStringVariable("v");
        Set<String> CV = new HashSet<>();

        Object closure = myStringVariable.close(store, CV);

        assertTrue(closure.getClass().equals(MyStringVariable.class));
    }

    @Test(expected=AttributeTypeException.class)
    public void closeNotInCVAttributeTypeException() throws AttributeTypeException, AttributeNotInStoreException {
        MyStringVariable myStringVariable = new MyStringVariable("v");
        Set<String> CV = new HashSet<>();
        CV.add("v");

        myStringVariable.close(store1, CV);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void closeNotInCVAttributeNotInStoreException() throws AttributeTypeException, AttributeNotInStoreException {
        MyStringVariable myStringVariable = new MyStringVariable("vv");
        Set<String> CV = new HashSet<>();
        CV.add("vv");

        myStringVariable.close(store1, CV);
    }
}