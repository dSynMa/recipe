package recipe.analysis;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;
public class BooleanLiteralValidationTest {
 @Test public void arbitraryValuesAreNotSilentlyConvertedToFalse() throws Exception {
  recipe.lang.types.Boolean type=recipe.lang.types.Boolean.getType();
  for(String invalid:new String[]{"1","0","2","ready","","trueSuffix"}) {
   assertThrows(recipe.lang.utils.exceptions.MismatchingTypeException.class,()->type.interpret(invalid));
   assertFalse(type.isValidValue(invalid));
  }
  assertEquals(true,type.interpret("TrUe"));
  assertEquals(false,type.interpret("FALSE"));
 }
 @Test public void numericGuardArgumentsAreRejectedForBooleanParameters() throws Exception {
  String model=source("supplier-locality").replace("agent Sender","guard allowed(a: boolean) := a;\nagent Sender");
  for(String value:new String[]{"0","1","2","0.5"})
   assertThrows(RuntimeException.class,()->parse(model+"\nSPEC allowed("+value+");\n"));
 }
 @Test public void guardNamesBeginningWithBooleanKeywordsRemainNames() throws Exception {
  for(String name:new String[]{"trueGuard","falseGuard"}) {
   String model=source("supplier-locality").replace("agent Sender","guard "+name+"(a: boolean) := !a;\nagent Sender");
   String output=ToNuXmv.transform(parse(model+"\nSPEC "+name+"(sender-sent);\n"));
   assertValid(output);
   assertCtl(output,output.substring(output.indexOf("LTLSPEC ")+8).trim(),true);
  }
 }
}
