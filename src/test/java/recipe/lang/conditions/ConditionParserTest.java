package recipe.lang.conditions;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConditionParserTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void conditionParsingValidationTest()
    {
        ConditionParser conditionParser = new ConditionParser();

        assertTrue( conditionParser.parse("!cond") );
        assertTrue( conditionParser.parse("! cond") );
        assertTrue( conditionParser.parse("!(cond)") );
        assertTrue( conditionParser.parse("(!cond)") );
        assertTrue( conditionParser.parse("!cond & cond") );
        assertTrue( conditionParser.parse("!(cond & cond)") );
        assertTrue( conditionParser.parse("!(cond & cond) | cond") );
        assertTrue( conditionParser.parse("(!(cond & cond)) | cond") );
        assertTrue( conditionParser.parse("((!(cond & cond)) | cond)") );
        assertTrue( conditionParser.parse("((!(cond & cond)) | (!(cond & cond)))") );
        assertTrue( conditionParser.parse("!(cond & cond) | (((!(cond & cond))))") );
        assertTrue( conditionParser.parse("!(cond & cond) | !(((!(cond & cond))))") );
        assertTrue( conditionParser.parse("!(cond & !cond) | !(((!(cond & cond))))") );
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void conditionParsingFalsificationTest()
    {
        ConditionParser conditionParser = new ConditionParser();

        assertTrue( !conditionParser.parse("!cond!") );
        assertTrue( !conditionParser.parse("!(cond & !cond") );
        assertTrue( !conditionParser.parse("!(cond & !cond))") );
        assertTrue( !conditionParser.parse("!cond & !cond | cond") );
    }
}