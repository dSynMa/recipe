package recipe.analysis;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;
public class GuardCallValidationTest {
 private String model() throws Exception {
  return source("supplier-locality").replace("agent Sender", "guard allowed(a: boolean) := !a;\nagent Sender");
 }
 @Test public void invalidArityIsRejectedWhileParsing() throws Exception {
  for(String call : new String[]{"allowed()", "allowed(TRUE, FALSE)"}) {
   RuntimeException error=assertThrows(RuntimeException.class,()->parse(model()+"\nSPEC "+call+";\n"));
   assertTrue(error.toString(),error.toString().contains("allowed"));
   assertFalse(error instanceof NullPointerException);
  }
 }
 @Test public void directConstructionChecksArity() throws Exception {
  parse(model());
  recipe.lang.types.Guard type=recipe.lang.types.Guard.getDefinition("allowed").getType();
  assertThrows(recipe.lang.utils.exceptions.MismatchingTypeException.class,
    ()->new recipe.lang.expressions.predicate.GuardReference(type,new recipe.lang.expressions.Expression[0]));
 }
 @Test public void validZeroAndOneArgumentCallsStillWork() throws Exception {
  String source=model().replace("agent Sender","guard always() := TRUE;\nagent Sender");
  assertValid(ToNuXmv.transform(parse(source+"\nSPEC allowed(sender-sent) & always();\n")));
 }
}
