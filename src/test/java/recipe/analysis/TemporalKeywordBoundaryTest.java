package recipe.analysis;
import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;
public class TemporalKeywordBoundaryTest {
 @Test public void uppercaseBooleanConstantsRemainAtoms() throws Exception {
  for(String formula : new String[]{"FALSE", "G FALSE", "X FALSE", "F TRUE"}) {
   String output=ToNuXmv.transform(parse(source("supplier-locality")+"\nSPEC "+formula+";\n"));
   assertValid(output);
   assertLtl(output,"("+output.substring(output.indexOf("LTLSPEC ")+8).trim()+") <-> ("+formula+")",true);
  }
 }
 @Test public void instanceNamesBeginningWithTemporalLettersRemainIdentifiers() throws Exception {
  for(String name : new String[]{"Gsender","Fsender","Xsender"}) {
   String model=source("supplier-locality").replace("Sender(sender, TRUE)","Sender("+name+", TRUE)");
   String output=ToNuXmv.transform(parse(model+"\nSPEC !"+name+"-sent;\n"));
   assertValid(output);
  }
 }

 @Test public void singleLetterTemporalBindersRemainIdentifiers() throws Exception {
  for(String name : new String[]{"G", "F", "X"}) {
   String formula = "/\\ " + name + ": Sender . (!" + name + "-sent)";
   String output=ToNuXmv.transform(parse(source("supplier-locality")+"\nSPEC "+formula+";\n"));
   assertValid(output);
   assertLtl(output, "("+output.substring(output.indexOf("LTLSPEC ")+8).trim()+") <-> (!sender-sent)",true);
  }
 }
}
