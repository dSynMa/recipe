package recipe.lang.expressions.predicate;

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

public class MyBooleanVariableTest {
    Store emptyStore;
    Store store;
    Store mismatchingTypeStore;
    Attribute attribute;

    @Before
    public void setUp() {
        attribute = new Attribute<>("v", Boolean.class);

        emptyStore = new Store();

        Map<String, Object> data = new HashMap<>();
        data.put("v", true);
        Map<String, Attribute<?>> attributes = new HashMap<>();
        attributes.put("v", attribute);
        store = new Store(data, attributes);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("v", 6);
        Map<String, Attribute<?>> attributes1 = new HashMap<>();
        Attribute attribute1 = new Attribute<>("v", Integer.class);

        attributes1.put("v", attribute1);
        mismatchingTypeStore = new Store(data1, attributes1);

    }

    @Test
    public void correctValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", false);
        MyBooleanVariable myBooleanVariable = new MyBooleanVariable("v");
        BooleanValue value = myBooleanVariable.valueIn(store);
        assertEquals(false, value.value);
    }

    @Test(expected=AttributeTypeException.class)
    public void valueInWrongTypeValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", 8);
        MyBooleanVariable myBooleanVariable = new MyBooleanVariable("v");
        myBooleanVariable.valueIn(store);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void valueInNotInStoreExceptionThrown() throws AttributeTypeException, AttributeNotInStoreException {
        MyBooleanVariable myBooleanVariable = new MyBooleanVariable("vv");
        myBooleanVariable.valueIn(store);
    }

    @Test
    public void closeInCV() throws AttributeTypeException, AttributeNotInStoreException {
        MyBooleanVariable myBooleanVariable = new MyBooleanVariable("v");
        Set<String> CV = new HashSet<>();
        CV.add("v");

        Object closure = myBooleanVariable.close(store, CV);

        assertTrue(closure.getClass().equals(BooleanValue.class));

        assertEquals(true, ((BooleanValue) myBooleanVariable.close(store, CV)).getValue());
    }

    @Test
    public void closeNotInCV() throws AttributeTypeException, AttributeNotInStoreException {
        MyBooleanVariable myBooleanVariable = new MyBooleanVariable("v");
        Set<String> CV = new HashSet<>();

        Object closure = myBooleanVariable.close(store, CV);

        assertTrue(closure.getClass().equals(MyBooleanVariable.class));
    }

    @Test(expected=AttributeTypeException.class)
    public void closeNotInCVAttributeTypeException() throws AttributeTypeException, AttributeNotInStoreException {
        MyBooleanVariable myBooleanVariable = new MyBooleanVariable("v");
        Set<String> CV = new HashSet<>();
        CV.add("v");

        myBooleanVariable.close(mismatchingTypeStore, CV);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void closeNotInCVAttributeNotInStoreException() throws AttributeTypeException, AttributeNotInStoreException {
        MyBooleanVariable myBooleanVariable = new MyBooleanVariable("vv");
        Set<String> CV = new HashSet<>();
        CV.add("vv");

        myBooleanVariable.close(mismatchingTypeStore, CV);
    }
}