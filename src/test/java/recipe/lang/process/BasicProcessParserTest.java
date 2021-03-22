package recipe.lang.process;

import org.junit.Test;
import static org.junit.Assert.*;

public class BasicProcessParserTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void validationTest()
    {
        ActionParser actionParser = new ActionParser();

        assertTrue( actionParser.parse("<cond>c!guard(msg)[y := w]") );
        assertTrue( actionParser.parse("<cond>c?(msg)[y := w]") );
        assertTrue( actionParser.parse("<cond>green!cond(v,w)[x := v, y := w]") );
        assertTrue( actionParser.parse("<cond>green?(v)[y := w]") );
        assertTrue( actionParser.parse("<cond>green?(v,w)[x := v, y := w]") );
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void falsificationTest()
    {
        ActionParser actionParser = new ActionParser();

        assertTrue( !actionParser.parse("<cond>c!condv,w)[x = v, y = w]") );
        assertTrue( !actionParser.parse("<cond>c!v[y = w]") );
        assertTrue( !actionParser.parse("<cond>c?(v)[y = w]") );
        assertTrue( !actionParser.parse("<cond>c?(v,w)[x = v, y = w]") );
    }
}