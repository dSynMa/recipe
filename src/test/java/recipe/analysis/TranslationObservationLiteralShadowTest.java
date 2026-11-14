package recipe.analysis;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;
public class TranslationObservationLiteralShadowTest {
 @Test public void booleanKeywordsCannotNameCommonVariablesOfAnyType() throws Exception {
  for (String name : new String[]{"true", "false", "TrUe", "FaLsE"})
   for (String type : new String[]{"boolean", "0..1", "channel"}) {
    String text = source("observation-forall-constant").replace("ready", name)
     .replace(name + ": boolean", name + ": " + type);
    RuntimeException error = assertThrows(RuntimeException.class, () -> parse(text));
    assertTrue(error.toString(), error.toString().contains("reserved"));
   }
 }
}
