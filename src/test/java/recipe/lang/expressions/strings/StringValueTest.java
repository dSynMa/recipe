package recipe.lang.expressions.strings;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.store.Attribute;
import recipe.lang.store.Store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringValueTest {
    Store emptyStore;
    Store store;

    @Before
    public void setUp() {
        Attribute attribute = new Attribute<>("v", String.class);
        String attributeVal = "val";

        emptyStore = new Store();

        Map<String, Object> data = new HashMap<>();
        data.put("v", attributeVal);
        Map<String, Attribute<?>> attributes = new HashMap<>();
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