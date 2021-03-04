package recipe.lang.actions;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ActionParserTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void actionParsingValidationTest()
    {
        ActionParser actionParser = new ActionParser();

        assertTrue( actionParser.parse("cond#receiver#!(v)[y = w]>(cond)") );
        assertTrue( actionParser.parse("cond#receiver#!(v,w)[x = v, y = w]>(cond)") );
        assertTrue( actionParser.parse("cond#receiver#?(v)[y = w]") );
        assertTrue( actionParser.parse("cond#receiver#?(v,w)[x = v, y = w]") );
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void actionParsingFalsificationTest()
    {
        ActionParser actionParser = new ActionParser();

        assertTrue( !actionParser.parse("cond#receiver#!v,w)[x = v, y = w]>(cond)") );
        assertTrue( !actionParser.parse("cond#receiver#!v[y = w]>(cond)") );
        assertTrue( !actionParser.parse("cond#receiver#?(v)[y = w]>(cond)") );
        assertTrue( !actionParser.parse("cond#receiver#?(v,w)[x = v, y = w]>(cond)") );
    }
}