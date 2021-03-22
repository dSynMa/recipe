package recipe.lang.expressions.strings;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringValueTest {
    Store emptyStore;
    Store store;

    @Before
    public void setUp() {
        TypedVariable attribute = new StringVariable("v");
        StringValue attributeVal = new StringValue("val");

        emptyStore = new Store();

        Map<String, TypedValue> data = new HashMap<>();
        data.put("v", attributeVal);
        Map<String, TypedVariable> attributes = new HashMap<>();
        attributes.put("v", attribute);
        store = new Store(data, attributes);
    }

    @Test
    public void correctValueReturned() throws AttributeTypeException, AttributeNotInStoreException {
        StringValue stringValue = new StringValue("val");
        StringValue value = stringValue.valueIn(store);
        assertTrue(value.value.equals("val"));
    }
}