package recipe.analysis;
import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;
public class TranslationGuardNameLiteralTest {
 private String model() throws Exception {
  return source("observation-forall-constant")
   .replace("channels: c", "channels: c, idle")
   .replace("agent Sender", "guard idle(flag: boolean) := flag;\nguard matches(link: channel) := link == idle;\nagent Sender");
 }
 @Test public void guardNameDoesNotShadowEnumPayloadLiteral() throws Exception {
  String text=model().replace("value: boolean", "value: channel")
   .replace("value := TRUE", "value := idle")
   .replace("receive: <value>", "receive: <value == idle>").replace("received := value", "received := TRUE");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
 }
 @Test public void guardNameDoesNotShadowEnumArgumentLiteral() throws Exception {
  String text=model().replace("send: <TRUE>", "send: <matches(idle)>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
 }
 @Test public void booleanKeywordsCannotNameGuards() throws Exception {
  for (String name : new String[]{"true", "false", "TRUE", "FALSE", "TrUe", "FaLsE"}) {
   String text = source("observation-forall-constant")
    .replace("agent Sender", "guard " + name + "(flag: boolean) := flag;\nagent Sender");
   RuntimeException error = org.junit.Assert.assertThrows(RuntimeException.class, () -> parse(text));
   org.junit.Assert.assertTrue(error.toString(), error.toString().contains("reserved"));
  }
 }
}
