package recipe.analysis;
import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;
public class TranslationChannelLiteralShadowTest {
 private String model(boolean send) throws Exception {
  String text=source("observation-forall-constant")
   .replace("channels: c", "channels: c, d")
   .replace("receive-guard: channel == c", "receive-guard: channel == d");
  return send ? text.replace("local: sent: boolean", "local: sent: boolean, c: channel")
   .replace("init: !sent", "init: !sent & c == d").replace("c?", "d?")
   : text.replace("local: received: boolean", "local: received: boolean, c: channel")
    .replace("init: !received", "init: !received & c == d").replace("c!", "d!");
 }
 @Test public void localVariablesCannotReuseChannelNames() throws Exception {
  for (boolean send : new boolean[]{true, false}) for (String type : new String[]{"channel", "boolean", "0..1"}) {
   String text = model(send).replace("c: channel", "c: " + type);
   IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse(text));
   assertTrue(error.getMessage(), error.getMessage().contains("Local variable 'c'")
       && error.getMessage().contains("channel constant"));
  }
 }
 private String distinctModel(boolean send) throws Exception {
  return model(send).replace("c: channel", "chosen: channel").replace("c == d", "chosen == d")
   .replace(send ? "c!" : "c?", send ? "chosen!" : "chosen?");
 }
 @Test public void distinctChannelVariablesSelectTheirStoredChannel() throws Exception {
  for (boolean send : new boolean[]{true, false}) {
   assertCtl(ToNuXmv.transform(parse(distinctModel(send))), "EX receiver-received", true);
   assertCtl(ToNuXmv.transform(parse(distinctModel(send).replace("chosen == d", "chosen != d"))),
       "EX receiver-received", false);
  }
 }
 @Test public void unshadowedLiteralChannelsRemainUsable() throws Exception {
  assertCtl(ToNuXmv.transform(parse(source("observation-forall-constant"))),"EX receiver-received",true);
 }
}
