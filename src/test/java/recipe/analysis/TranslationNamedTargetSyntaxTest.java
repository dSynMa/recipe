package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationNamedTargetSyntaxTest {
    @Test public void malformedPredicatesCannotBecomeNamedSupplyTargets() throws Exception {
        for (String target : new String[]{"(@ready + 1)", "(@missing)", "(first second)", "(@ready & )"}) {
            String text = source("send-observation-on-get").replace("SUPPLY@(@ready)", "SUPPLY@" + target);
            assertTrue(target, recipe.lang.System.parser().end().parse(text).isFailure());
        }
    }

    @Test public void malformedPredicatesCannotBecomeNamedGetTargets() throws Exception {
        for (String target : new String[]{"(@ready + 1)", "(@missing)", "(first second)", "(@ready & )"}) {
            String text = source("send-observation-on-get").replace("GET@TRUE", "GET@" + target);
            assertTrue(target, recipe.lang.System.parser().end().parse(text).isFailure());
        }
    }

    @Test public void validNamedTargetsRetainWhitespaceAndParenthesesSupport() throws Exception {
        for (String supply : new String[]{"first", "( first )"}) for (String get : new String[]{"sender", "( sender )"}) {
            String text = source("supplier-locality").replace("SUPPLY@first", "SUPPLY@" + supply)
                .replace("GET@TRUE", "GET@" + get);
            assertCtl(ToNuXmv.transform(parse(text)), "EX first-received", true);
            assertCtl(ToNuXmv.transform(parse(text)), "EX second-received", false);
        }
    }
}
