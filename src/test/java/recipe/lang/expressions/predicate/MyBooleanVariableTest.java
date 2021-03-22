package recipe.lang.expressions.predicate;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberValue;
import recipe.lang.expressions.arithmetic.NumberVariable;
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
    TypedVariable attribute;

    @Before
    public void setUp() {
        attribute = new BooleanVariable("v");

        emptyStore = new Store();

        Map<String, TypedValue> data = new HashMap<>();
        data.put("v", new BooleanValue(true));
        Map<String, TypedVariable> attributes = new HashMap<>();
        attributes.put("v", attribute);
        store = new Store(data, attributes);

        Map<String, TypedValue> data1 = new HashMap<>();
        data1.put("v", new NumberValue(6));
        Map<String, TypedVariable> attributes1 = new HashMap<>();
        TypedVariable attribute1 = new NumberVariable("v");

        attributes1.put("v", attribute1);
        mismatchingTypeStore = new Store(data1, attributes1);

    }

    @Test
    public void correctValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", new BooleanValue(false));
        MyBooleanVariable myBooleanVariable = new MyBooleanVariable("v");
        BooleanValue value = myBooleanVariable.valueIn(store);
        assertEquals(false, value.value);
    }

    @Test(expected=AttributeTypeException.class)
    public void valueInWrongTypeValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", new NumberValue(8));
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