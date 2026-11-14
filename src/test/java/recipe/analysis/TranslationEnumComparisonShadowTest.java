package recipe.analysis;
import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;
public class TranslationEnumComparisonShadowTest {
 @Test public void numericVariablesShadowEnumLiteralsInComparisons() throws Exception {
  String text=source("observation-forall-constant")
   .replace("channels: c", "channels: c\nenum direction {left, right}")
   .replace("local: sent: boolean", "local: sent: boolean, left: 0..1, right: 0..1")
   .replace("init: !sent", "init: !sent & left == 0 & right == 0")
   .replace("send: <TRUE>", "send: <left == right>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
 }
 @Test public void booleanVariablesShadowEnumLiteralsInComparisons() throws Exception {
  String text=source("observation-forall-constant")
   .replace("channels: c", "channels: c\nenum direction {left, right}")
   .replace("local: sent: boolean", "local: sent: boolean, left: boolean, right: boolean")
   .replace("init: !sent", "init: !sent & !left & !right")
   .replace("send: <TRUE>", "send: <left == right>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
 }

 @Test public void numericMessageFieldsShadowAgentNamesInObservations() throws Exception {
  String text=source("observation-forall-constant")
   .replace("message-structure: value: boolean", "message-structure: left: 0..1, right: 0..1")
   .replace("value := TRUE", "left := 0, right := 0")
   .replace("receive: <value>", "receive: <left == right>")
   .replace("received := value", "received := TRUE")
   .replace("Sender(sender, TRUE)", "Sender(left, TRUE)")
   .replace("Receiver(receiver, TRUE)", "Receiver(right, TRUE)")
   .replace("forall(TRUE)", "left == right");
  assertCtl(ToNuXmv.transform(parse(text)),"EX (right-received & obs0)",true);
 }
 @Test public void numericGuardParametersShadowEnumLiterals() throws Exception {
  String text=source("observation-forall-constant")
   .replace("channels: c", "channels: c, left, right")
   .replace("agent Sender", "guard same(left: 0..1, right: 0..1) := left == right;\nagent Sender")
   .replace("send: <TRUE>", "send: <same(0, 0)>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
  assertCtl(ToNuXmv.transform(parse(text.replace("same(0, 0)","same(0, 1)"))),"EX receiver-received",false);
 }
 @Test public void unshadowedEnumLiteralsRemainConstants() throws Exception {
  String text=source("observation-forall-constant")
   .replace("channels: c", "channels: c, left, right")
   .replace("send: <TRUE>", "send: <left != right>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
 }
}
