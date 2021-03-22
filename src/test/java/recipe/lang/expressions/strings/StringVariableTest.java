package recipe.lang.expressions.strings;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberValue;
import recipe.lang.store.Store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class StringVariableTest {
    Store emptyStore;
    Store store;
    Store mismatchingTypeStore;
    TypedVariable attribute;
    TypedValue attributeVal;

    @Before
    public void setUp() {
        attribute = new StringVariable("v");
        attributeVal = new StringValue("val");

        emptyStore = new Store();

        Map<String, TypedValue> data = new HashMap<>();
        data.put("v", attributeVal);
        Map<String, TypedVariable> attributes = new HashMap<>();
        attributes.put("v", attribute);
        store = new Store(data, attributes);

        Map<String, TypedValue> data1 = new HashMap<>();
        data1.put("v", new NumberValue(6));
        Map<String, TypedVariable> attributes1 = new HashMap<>();
        TypedVariable attribute1 = new StringVariable("v");

        attributes1.put("v", attribute1);
        mismatchingTypeStore = new Store(data1, attributes1);
    }

    @Test
    public void correctValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", new StringValue("example"));
        StringVariable stringVariable = new StringVariable("v");
        StringValue value = stringVariable.valueIn(store);
        assertTrue(value.value.equals("example"));
    }

    @Test(expected=AttributeTypeException.class)
    public void valueInWrongTypeValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", new NumberValue(8));
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

        assertEquals(attributeVal, closure);
    }

    @Test(expected=AttributeTypeException.class)
    public void closeNotInCVAttributeTypeException() throws AttributeTypeException, AttributeNotInStoreException {
        StringVariable stringVariable = new StringVariable("v");
        Set<String> CV = new HashSet<>();

        stringVariable.close(mismatchingTypeStore, CV);
    }
}