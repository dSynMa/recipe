package recipe.analysis;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;
public class TranslationGuardArithmeticDomainTest {
 private String model(String type,String arg) throws Exception {
  return source("observation-forall-constant")
   .replace("agent Sender", "guard inDomain(n: "+type+") := n >= 0;\nagent Sender")
   .replace("send: <TRUE>", "send: <inDomain("+arg+")>");
 }
 @Test public void arithmeticCannotBypassBoundedParameterDomain() throws Exception {
  for(String arg:new String[]{"2", "1 + 1", "0 - 1", "3 / 2"})
   assertThrows(IllegalArgumentException.class,()->parse(model("0..1",arg)));
 }
 @Test public void arithmeticCannotPassFractionToIntegerParameter() throws Exception {
  assertThrows(IllegalArgumentException.class,()->parse(model("integer","1 / 2")));
 }
 @Test public void symbolicArithmeticCannotWidenTheParameterDomain() throws Exception {
  String text=model("0..1","score + 1")
   .replace("local: sent: boolean", "local: sent: boolean, score: 0..1")
   .replace("init: !sent", "init: !sent & score == 0");
  assertThrows(IllegalArgumentException.class,()->parse(text));
  assertCtl(ToNuXmv.transform(parse(text.replace("score + 1", "score + 0"))),"EX receiver-received",true);
 }
 @Test public void containedArithmeticArgumentsRemainValid() throws Exception {
  for(String arg:new String[]{"0 + 1", "2 - 1", "1 * 1", "2 / 2"})
   assertCtl(ToNuXmv.transform(parse(model("0..1",arg))),"EX receiver-received",true);
 }
}
