package recipe.analysis;
import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;
import static org.junit.Assert.*;
public class TranslationEnumGuardArgumentTest {
 @Test public void booleanLookingEnumArgumentsRetainDeclaredParameterType() throws Exception {
  for(String literal : new String[]{"false", "true"}) {
   String text=source("observation-forall-constant")
    .replace("channels: c", "channels: c, " + literal)
    .replace("agent Sender", "guard matches(valueArg: channel) := valueArg == " + literal + ";\nagent Sender")
    .replace("send: <TRUE>", "send: <matches(" + literal + ")>");
   assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
  }
 }

 @Test public void mixedNestedCallsKeepEachParameterDomain() throws Exception {
  String text=source("observation-forall-constant")
   .replace("channels: c", "channels: c, false, true")
   .replace("agent Sender", "guard both(flag: boolean, link: channel) := flag & link == false;\nguard identity(flag: boolean) := flag;\nagent Sender")
   .replace("send: <TRUE>", "send: <both(identity(TRUE), false)>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
  assertCtl(ToNuXmv.transform(parse(text.replace("identity(TRUE)", "identity(false)"))),"EX receiver-received",false);
 }
 @Test public void enumVariablesTakePrecedenceOverLiteralSpellings() throws Exception {
  String text=source("observation-forall-constant")
   .replace("channels: c", "channels: c, false, d")
   .replace("agent Sender", "guard isD(link: channel) := link == d;\nagent Sender")
   .replace("local: sent: boolean", "local: sent: boolean, false: channel")
   .replace("init: !sent", "init: !sent & false == d")
   .replace("send: <TRUE>", "send: <isD(false)>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
 }
 @Test public void enumParametersStillRejectWrongCategoriesAndArity() throws Exception {
  String base=source("observation-forall-constant")
   .replace("channels: c", "channels: c, false")
   .replace("agent Sender", "guard matches(link: channel) := link == false;\nagent Sender");
  for(String arguments:new String[]{"2", "TRUE", "", "false, false"}) {
   String text=base.replace("send: <TRUE>", "send: <matches("+arguments+")>");
   assertThrows(IllegalArgumentException.class, () -> parse(text));
  }
 }
}
