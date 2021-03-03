package recipe;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class RecipeLangParserTest
{

    /**
     * Rigorous Test :-)
     */
    @Test
    public void conditionParsingValidationTest()
    {
        RecipeLangParser recipeLangParser = new RecipeLangParser();

        assertTrue( recipeLangParser.parseCondition("!cond") );
        assertTrue( recipeLangParser.parseCondition("! cond") );
        assertTrue( recipeLangParser.parseCondition("!(cond)") );
        assertTrue( recipeLangParser.parseCondition("(!cond)") );
        assertTrue( recipeLangParser.parseCondition("!cond & cond") );
        assertTrue( recipeLangParser.parseCondition("!(cond & cond)") );
        assertTrue( recipeLangParser.parseCondition("!(cond & cond) | cond") );
        assertTrue( recipeLangParser.parseCondition("(!(cond & cond)) | cond") );
        assertTrue( recipeLangParser.parseCondition("((!(cond & cond)) | cond)") );
        assertTrue( recipeLangParser.parseCondition("((!(cond & cond)) | (!(cond & cond)))") );
        assertTrue( recipeLangParser.parseCondition("!(cond & cond) | (((!(cond & cond))))") );
        assertTrue( recipeLangParser.parseCondition("!(cond & cond) | !(((!(cond & cond))))") );
        assertTrue( recipeLangParser.parseCondition("!(cond & !cond) | !(((!(cond & cond))))") );
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void conditionParsingFalsificationTest()
    {
        RecipeLangParser recipeLangParser = new RecipeLangParser();

        assertTrue( !recipeLangParser.parseCondition("!cond!") );
        assertTrue( !recipeLangParser.parseCondition("!(cond & !cond") );
        assertTrue( !recipeLangParser.parseCondition("!(cond & !cond))") );
        assertTrue( !recipeLangParser.parseCondition("!cond & !cond | cond") );
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void actionParsingValidationTest()
    {
        RecipeLangParser recipeLangParser = new RecipeLangParser();

        assertTrue( recipeLangParser.parseAction("cond#receiver#!(v)[y = w]>(cond)") );
        assertTrue( recipeLangParser.parseAction("cond#receiver#!(v,w)[x = v, y = w]>(cond)") );
        assertTrue( recipeLangParser.parseAction("cond#receiver#?(v)[y = w]") );
        assertTrue( recipeLangParser.parseAction("cond#receiver#?(v,w)[x = v, y = w]") );
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void actionParsingFalsificationTest()
    {
        RecipeLangParser recipeLangParser = new RecipeLangParser();

        assertTrue( !recipeLangParser.parseAction("cond#receiver#!v,w)[x = v, y = w]>(cond)") );
        assertTrue( !recipeLangParser.parseAction("cond#receiver#!v[y = w]>(cond)") );
        assertTrue( !recipeLangParser.parseAction("cond#receiver#?(v)[y = w]>(cond)") );
        assertTrue( !recipeLangParser.parseAction("cond#receiver#?(v,w)[x = v, y = w]>(cond)") );
    }
}
