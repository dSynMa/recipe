package recipe.analysis;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;
public class TranslationBooleanLiteralShadowTest {
 @Test public void booleanKeywordsCannotNameDeclarations() throws Exception {
  for (String name : new String[]{"true", "false", "TRUE", "FALSE", "TrUe", "FaLsE"}) {
   String base = source("observation-forall-constant");
   String[] models = {
    base.replace("sent", name),
    base.replace("value", name),
    base.replace("ready", name),
    base.replace("agent Sender", "guard identity(" + name + ": boolean) := TRUE;\nagent Sender"),
    base.replace("channels: c", "channels: c, " + name),
    base.replace("channels: c", "channels: c\nenum mode {" + name + "}"),
    base.replace("channels: c", "channels: c\nenum " + name + " {idle}"),
    base.replace("Sender", name),
    base.replace("Sender(sender,", "Sender(" + name + ","),
    base.replace("send:", name + ":"),
    base.replace("receive:", name + ":"),
    base.replace("send: <TRUE> c!", name + ": <TRUE> SUPPLY@"),
    base.replace("receive: <value> c?", name + ": <value> GET@TRUE")
   };
   for (String model : models) {
    try {
     assertTrue(model, recipe.lang.System.parser().end().parse(model).isFailure());
    } catch (IllegalArgumentException error) {
     assertTrue(error.toString(), error.toString().contains("reserved"));
    }
   }
  }
 }
 @Test public void prefixesAndMixedCaseLiteralsRemainValid() throws Exception {
  String text = source("observation-forall-constant").replace("sent", "trueFlag")
   .replace("received", "falseFlag").replace("TRUE", "TrUe").replace("FALSE", "FaLsE");
  assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-falseFlag", true);
 }
}
