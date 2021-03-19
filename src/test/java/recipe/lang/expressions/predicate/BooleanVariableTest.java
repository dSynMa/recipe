package recipe.lang.expressions.predicate;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.expressions.strings.StringVariable;
import recipe.lang.store.Attribute;
import recipe.lang.store.Store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BooleanVariableTest {
    Store emptyStore;
    Store store;
    Store mismatchingTypeStore;
    Attribute attribute;
    Boolean attributeVal;
    BooleanVariable booleanVariable;

    @Before
    public void setUp() throws Exception {
        attribute = new Attribute<>("v", Boolean.class);
        attributeVal = false;

        emptyStore = new Store();
        booleanVariable = new BooleanVariable("v");

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
        mismatchingTypeStore = new Store(data1, attributes1);
    }

    @Test
    public void correctValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue(booleanVariable.name, !attributeVal);
        assertEquals(new BooleanValue(!attributeVal), booleanVariable.valueIn(store));
    }

    @Test(expected=AttributeTypeException.class)
    public void valueInWrongTypeValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", 8);
        booleanVariable.valueIn(store);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void valueInNotInStoreExceptionThrown() throws AttributeTypeException, AttributeNotInStoreException {
        BooleanVariable stringVariable = new BooleanVariable("vv");
        stringVariable.valueIn(store);
    }

    @Test(expected=AttributeNotInStoreException.class)
    public void varNotInCVAndNotAttribute() throws AttributeTypeException, AttributeNotInStoreException {
        BooleanVariable booleanVariable = new BooleanVariable("x");
        Set<String> CV = new HashSet<>();

        booleanVariable.close(store, CV);
    }

    @Test
    public void varNotInCVAndAttribute() throws AttributeTypeException, AttributeNotInStoreException {
        Set<String> CV = new HashSet<>();

        Expression closure = booleanVariable.close(store, CV);
        BooleanValue val = new BooleanValue(attributeVal);

        assertEquals(val, closure);
    }

    @Test(expected=AttributeTypeException.class)
    public void closeNotInCVAttributeTypeException() throws AttributeTypeException, AttributeNotInStoreException {
        Set<String> CV = new HashSet<>();

        booleanVariable.close(mismatchingTypeStore, CV);
    }
}