package recipe.analysis;
import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;
public class TranslationChannelLocalTest {
 private String model(String type, String initial, String selection) throws Exception {
  return source("observation-forall-constant")
   .replace("channels: c", "channels: c, d")
   .replace("sent: boolean", "sent: boolean, channel: " + type)
   .replace("init: !sent", "init: !sent & " + initial)
   .replace("c! (@ready)", "c! (@ready & " + selection + ")");
 }
 @Test public void booleanLocalChannelInSendSelectionRetainsSenderScope() throws Exception {
  assertCtl(ToNuXmv.transform(parse(model("boolean", "channel", "channel"))), "EX receiver-received",true);
  assertCtl(ToNuXmv.transform(parse(model("boolean", "!channel", "channel"))), "EX receiver-received",false);
 }
 @Test public void enumLocalChannelDoesNotBecomeTheOutgoingChannel() throws Exception {
  assertCtl(ToNuXmv.transform(parse(model("channel", "channel == d", "channel == d"))), "EX receiver-received",true);
  assertCtl(ToNuXmv.transform(parse(model("channel", "channel == d", "channel == c"))), "EX receiver-received",false);
 }
 @Test public void explicitMacroArgumentsKeepLocalChannelScope() throws Exception {
  String text=model("boolean", "channel", "identity(channel)")
   .replace("agent Sender", "guard identity(flag: boolean) := flag;\nagent Sender");
  assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received",true);
 }
 @Test public void currentChannelMacroWithoutLocalChannelRemainsValid() throws Exception {
  for (String channel : new String[]{"c", "d"}) {
   String text=source("observation-forall-constant").replace("channels: c", "channels: c, d")
    .replace("agent Sender", "guard onChannel() := channel == " + channel + ";\nagent Sender")
    .replace("c! (@ready)", "c! (@ready & onChannel())");
   assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received",channel.equals("c"));
  }
 }
 @Test public void directCurrentChannelSelectionUsesOutgoingChannel() throws Exception {
  for (String channel : new String[]{"c", "d"}) {
   String text=source("observation-forall-constant").replace("channels: c", "channels: c, d")
    .replace("c! (@ready)", "c! (@ready & channel == " + channel + ")");
   assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received",channel.equals("c"));
  }
 }
}
