package recipe.analysis;
import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;
public class GuardDeclarationWhitespaceTest {
 @Test public void whitespaceDoesNotBecomePartOfTheGuardName() throws Exception {
  for(String space : new String[]{"", " ", "\t", "\n"}) {
   String model=source("supplier-locality").replace("agent Sender", "guard allowed"+space+"(a: boolean) := !a;\nagent Sender");
   String output=ToNuXmv.transform(parse(model+"\nSPEC allowed(sender-sent);\n"));
   assertValid(output);
   assertCtl(output, output.substring(output.indexOf("LTLSPEC ")+8).trim(),true);
  }
 }
}
