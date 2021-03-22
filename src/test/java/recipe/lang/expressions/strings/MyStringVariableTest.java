package recipe.lang.expressions.strings;

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

public class MyStringVariableTest {
    Store emptyStore;
    Store store;
    Store mismatchingTypeStore;
    TypedVariable attribute;

    @Before
    public void setUp() {
        attribute = new StringVariable("v");

        emptyStore = new Store();

        Map<String, TypedValue> data = new HashMap<>();
        data.put("v", new StringValue("val"));
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
        store.setValue("v", new StringValue("example"));
        MyStringVariable myStringVariable = new MyStringVariable("v");
        StringValue value = myStringVariable.valueIn(store);
        assertTrue(value.value.equals("example"));
    }

    @Test(expected=AttributeTypeException.class)
    public void valueInWrongTypeValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", new NumberValue(8));
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

        assertTrue(((StringValue) myStringVariable.close(store, CV)).getValue().equals("val"));
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

        myStringVariable.close(mismatchingTypeStore, CV);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void closeNotInCVAttributeNotInStoreException() throws AttributeTypeException, AttributeNotInStoreException {
        MyStringVariable myStringVariable = new MyStringVariable("vv");
        Set<String> CV = new HashSet<>();
        CV.add("vv");

        myStringVariable.close(mismatchingTypeStore, CV);
    }
}