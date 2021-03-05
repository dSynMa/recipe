package recipe.lang.actions;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ActionParserTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void validationTest()
    {
        ActionParser actionParser = new ActionParser();

        assertTrue( actionParser.parse("<cond>c!guard(msg)[y := w]") );
        assertTrue( actionParser.parse("<cond>c?(msg)[y := w]") );
        assertTrue( actionParser.parse("cond#receiver#!(v,w)[x = v, y = w]>(cond)") );
        assertTrue( actionParser.parse("cond#receiver#?(v)[y = w]") );
        assertTrue( actionParser.parse("cond#receiver#?(v,w)[x = v, y = w]") );
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void falsificationTest()
    {
        ActionParser actionParser = new ActionParser();

        assertTrue( !actionParser.parse("cond#receiver#!v,w)[x = v, y = w]>(cond)") );
        assertTrue( !actionParser.parse("cond#receiver#!v[y = w]>(cond)") );
        assertTrue( !actionParser.parse("cond#receiver#?(v)[y = w]>(cond)") );
        assertTrue( !actionParser.parse("cond#receiver#?(v,w)[x = v, y = w]>(cond)") );
    }
}