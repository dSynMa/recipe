package recipe.lang.expressions.predicate;

import org.junit.Before;
import org.junit.Test;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Store;

import java.util.HashSet;

import static org.junit.Assert.*;

public class BooleanValueTest {
    Store emptyStore;
    Store store;
    BooleanValue booleanValue;

    @Before
    public void setUp() throws Exception {
        booleanValue = new BooleanValue(true);
    }

    @Test
    public void valueIn() throws AttributeTypeException, AttributeNotInStoreException {
        assertEquals(booleanValue, booleanValue.valueIn(store));
    }

    @Test
    public void valueInEmptyStore() throws AttributeTypeException, AttributeNotInStoreException {
        assertEquals(booleanValue, booleanValue.valueIn(emptyStore));
    }

    @Test
    public void close() throws AttributeNotInStoreException {
        assertEquals(booleanValue, booleanValue.close(store, new HashSet<>()));
    }

    @Test
    public void closeEmptyStore() throws AttributeNotInStoreException {
        assertEquals(booleanValue, booleanValue.close(emptyStore, new HashSet<>()));
    }
}