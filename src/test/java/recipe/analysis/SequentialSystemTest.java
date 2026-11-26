package recipe.analysis;

import org.junit.Test;
import org.petitparser.parser.Parser;
import flak.Query;
import flak.Request;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import recipe.Config;
import recipe.lang.types.Enum;
import recipe.lang.types.Guard;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SequentialSystemTest {
    private String model(String tag, boolean allowed) throws Exception {
        return source("supplier-locality")
            .replace("sender", tag+"Sender").replace("first", tag+"First").replace("second", tag+"Second")
            .replace("channels: c", "channels: "+tag+"Channel")
            .replace("c!",tag+"Channel!").replace("c?",tag+"Channel?").replace("channel == c", "channel == "+tag+"Channel")
            .replace("message-structure:","enum "+tag+"Enum {"+tag+"Value}\nmessage-structure:")
            .replace("agent Sender", "guard gate() := "+(allowed?"TRUE":"FALSE")+";\nguard "+tag+"Guard() := TRUE;\nagent Sender")
            .replace("<TRUE>", "<gate()>");
    }
    @Test
    public void freshParsersReplaceAllRegistries() throws Exception {
        String previous = null;
        for (String tag : new String[]{"alpha","beta","gamma"}) {
            boolean allowed = !tag.equals("beta");
            var system = parse(model(tag, allowed));
            assertTrue(Enum.exists(tag+"Enum"));
            assertNotNull(Guard.getDefinition(tag+"Guard"));
            assertEquals(3,Config.getAgentType().getAllValues().size());
            String output = ToNuXmv.transform(system);
            assertCtl(output,"EX "+tag+"Sender-sent",allowed);
            if (previous != null) {
                assertFalse(Enum.exists(previous+"Enum"));
                assertNull(Guard.getDefinition(previous+"Guard"));
                assertFalse(output.contains(previous+"Sender"));
                assertFalse(output.contains(previous+"Channel"));
            }
            previous = tag;
        }
    }
    @Test
    public void reusedParserDoesNotRetainOldDeclarations() throws Exception {
        Parser parser = recipe.lang.System.parser().end();
        parser.parse(model("alpha",true)).get();
        parser.parse(model("gamma", true)).get();
        assertFalse(Enum.exists("alphaEnum"));
        assertTrue(parser.parse("channels: partial\nmessage-structure:").isFailure());
        recipe.lang.System current = parser.parse(model("beta", false)).get();
        assertCtl(ToNuXmv.transform(current),"EX betaSender-sent",false);
        assertFalse("Reused system parser retains alphaEnum", Enum.exists("alphaEnum"));
        assertFalse(Enum.exists("gammaEnum"));
    }
    private Field field(Class<?> type,String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private Request request(Map<String,String> values) {
        Object query=Proxy.newProxyInstance(Query.class.getClassLoader(),new Class<?>[]{Query.class},(p,m,a)->m.getName().equals("get")?values.get(a[0]):null);
        return (Request)Proxy.newProxyInstance(Request.class.getClassLoader(),new Class<?>[]{Request.class},(p,m,a)->m.getName().equals("getQuery")?query:null);
    }
    private void failedReplacement(String endpoint) throws Exception {
        Class<?> type = Class.forName("Server");
        Object server = type.getDeclaredConstructor().newInstance();
        var old = parse(model("alpha",true));
        NuXmvInteraction checker = new NuXmvInteraction(old);
        try {
            field(type,"system").set(server,old);
            field(type,"interpreter").set(server,new recipe.interpreter.Interpreter(old));
            field(type,"nuXmvInteraction").set(server,checker);
            String result=(String)type.getMethod(endpoint,Request.class).invoke(server,request(Map.of("script","channels: broken\nmessage-structure:","symbolic","true")));
            assertTrue(result,result.contains("error"));
            assertNull("Failed replacement retains obsolete system with overwritten registries",field(type,"system").get(server));
            assertNull(field(type,"interpreter").get(server));
            assertNull(field(type,"nuXmvInteraction").get(server));
            assertFalse("Old checker must terminate", checker.process.isAlive());
            String recovered=(String)type.getMethod("setSystem",Request.class).invoke(server,
                request(Map.of("script",model("beta",false))));
            assertFalse(recovered,recovered.contains("error"));
            String output = ToNuXmv.transform((recipe.lang.System)field(type,"system").get(server));
            assertCtl(output,"EX betaSender-sent",false);
            assertFalse(Enum.exists("alphaEnum"));
        } finally {
            checker.close();
            ((ExecutorService) field(type, "service").get(server)).shutdownNow();
        }
    }
    @Test
    public void failedSelectionDoesNotKeepOldSystem() throws Exception {
        failedReplacement("setSystem");
    }
    @Test
    public void failedBuildDoesNotKeepOldSystem() throws Exception {
        failedReplacement("buildModel");
    }
}
