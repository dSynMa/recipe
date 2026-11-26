package recipe.analysis;
import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;
import static org.junit.Assert.*;
public class TranslationEnumGuardArgumentTest {
 @Test public void prefixedBooleanEnumArgumentsRetainDeclaredParameterType() throws Exception {
  for(String literal : new String[]{"falseValue", "trueValue"}) {
   String text=source("observation-forall-constant")
    .replace("channels: c", "channels: c, " + literal)
    .replace("agent Sender", "guard matches(valueArg: channel) := valueArg == " + literal + ";\nagent Sender")
    .replace("send: <TRUE>", "send: <matches(" + literal + ")>");
   assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
  }
 }

 @Test public void mixedNestedCallsKeepEachParameterDomain() throws Exception {
  String text=source("observation-forall-constant")
   .replace("channels: c", "channels: c, falseValue, trueValue")
   .replace("agent Sender", "guard both(flag: boolean, link: channel) := flag & link == falseValue;\nguard identity(flag: boolean) := flag;\nagent Sender")
   .replace("send: <TRUE>", "send: <both(identity(TRUE), falseValue)>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
  assertCtl(ToNuXmv.transform(parse(text.replace("identity(TRUE)", "identity(FALSE)"))),"EX receiver-received",false);
 }
 @Test public void enumVariablesTakePrecedenceOverLiteralSpellings() throws Exception {
  String text=source("observation-forall-constant")
   .replace("channels: c", "channels: c\nenum shade {falseValue, d}")
   .replace("agent Sender", "guard isD(link: shade) := link == d;\nagent Sender")
   .replace("local: sent: boolean", "local: sent: boolean, falseValue: shade")
   .replace("init: !sent", "init: !sent & falseValue == d")
   .replace("send: <TRUE>", "send: <isD(falseValue)>");
  assertCtl(ToNuXmv.transform(parse(text)),"EX receiver-received",true);
 }
 @Test public void enumParametersStillRejectWrongCategoriesAndArity() throws Exception {
  String base=source("observation-forall-constant")
   .replace("channels: c", "channels: c, falseValue")
   .replace("agent Sender", "guard matches(link: channel) := link == falseValue;\nagent Sender");
  for(String arguments:new String[]{"2", "TRUE", "", "falseValue, falseValue"}) {
   String text=base.replace("send: <TRUE>", "send: <matches("+arguments+")>");
   assertThrows(IllegalArgumentException.class, () -> parse(text));
  }
 }
}
