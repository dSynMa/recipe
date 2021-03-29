package recipe.lang.expressions.predicate;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberValue;
import recipe.lang.expressions.arithmetic.NumberVariable;
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
    BooleanVariable attribute;
    BooleanValue attributeVal;
    BooleanVariable booleanVariable;

    @Before
    public void setUp() throws Exception {
        attribute = new BooleanVariable("v");
        attributeVal = new BooleanValue(false);

        emptyStore = new Store();
        booleanVariable = new BooleanVariable("v");

        Map<String, Expression> data = new HashMap<>();
        data.put("v", attributeVal);
        Map<String, TypedVariable> attributes = new HashMap<>();
        attributes.put("v", attribute);
        store = new Store(data, attributes);

        Map<String, Expression> data1 = new HashMap<>();
        data1.put("v", new NumberValue(6));
        Map<String, TypedVariable> attributes1 = new HashMap<>();
        TypedVariable attribute1 = new NumberVariable("v");

        attributes1.put("v", attribute1);
        mismatchingTypeStore = new Store(data1, attributes1);
    }

    @Test
    public void correctValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue(booleanVariable.name, new BooleanValue(!attributeVal.getValue()));
        assertEquals(new BooleanValue(!attributeVal.getValue()), booleanVariable.valueIn(store));
    }

    @Test(expected=AttributeTypeException.class)
    public void valueInWrongTypeValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        store.setValue("v", new NumberValue(8));
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
        BooleanValue val = new BooleanValue(attributeVal.getValue());

        assertEquals(val, closure);
    }

    @Test(expected=AttributeTypeException.class)
    public void closeNotInCVAttributeTypeException() throws AttributeTypeException, AttributeNotInStoreException {
        Set<String> CV = new HashSet<>();

        booleanVariable.close(mismatchingTypeStore, CV);
    }
}