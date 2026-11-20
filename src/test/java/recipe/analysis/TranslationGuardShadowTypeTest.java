package recipe.analysis;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;
public class TranslationGuardShadowTypeTest {
 @Test public void shadowingParameterCannotRetainFormerEnumType() throws Exception {
  String text=source("observation-forall-constant")
   .replace("agent Sender", "guard bad(channel: boolean) := channel == c;\nagent Sender");
  assertTrue("Ill-typed Boolean/enum comparison accepted", recipe.lang.System.parser().end().parse(text).isFailure());
 }
 @Test public void shadowingParameterWorksWithItsOwnType() throws Exception {
  String text=source("observation-forall-constant")
   .replace("agent Sender", "guard good(channel: boolean) := channel == TRUE;\nagent Sender")
   .replace("send: <TRUE>", "send: <good(TRUE)>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
 }
}
