package recipe.analysis;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayServerErrorTest {
    private Field field(Class<?> type,String name) throws Exception {
        Field field=type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private String request(String trace) throws Exception {
        Class<?> serverType=Class.forName("Server");
        Object server=serverType.getDeclaredConstructor().newInstance();
        Class<?> queryType=Class.forName("flak.Query");
        Object query=Proxy.newProxyInstance(queryType.getClassLoader(),new Class<?>[]{queryType},
            (proxy,method,args)->method.getName().equals("get") ? trace : null);
        Class<?> requestType=Class.forName("flak.Request");
        Object request=Proxy.newProxyInstance(requestType.getClassLoader(),new Class<?>[]{requestType},
            (proxy,method,args)->method.getName().equals("getQuery") ? query : null);
        try {
            field(serverType,"system").set(server,parse(source("observation-forall-constant")));
            return (String)serverType.getMethod("loadInterpreterJSON",requestType).invoke(server,request);
        } finally {
            ((ExecutorService)field(serverType,"service").get(server)).shutdownNow();
        }
    }
    @Test(timeout=20000)
    public void failedInitialSelectionReturnsValidJsonDiagnostic() throws Exception {
        String response=request("[{sender:{sent:true},receiver:{received:false}}]");
        JSONObject json=new JSONObject(response);
        assertTrue(response,json.has("error"));
        assertTrue(json.getString("error"),json.getString("error").contains("Cannot select an initial state"));
    }
    @Test(timeout=20000)
    public void validReplayReturnsTraceAndFinalState() throws Exception {
        JSONObject response = new JSONObject(request("[{sender:{sent:false},receiver:{received:false}}]"));
        assertFalse(response.toString(), response.has("error"));
        assertEquals(1, response.getJSONArray("trace").length());
        assertTrue(response.has("state"));
        assertTrue(response.has("transitions"));
        assertTrue(response.has("svgs"));
    }
    @Test(timeout=20000)
    public void malformedTraceReturnsValidJsonDiagnostic() throws Exception {
        JSONObject response = new JSONObject(request("not a JSON array"));
        assertTrue(response.toString(), response.has("error"));
        assertFalse(response.getString("error").isEmpty());
    }
}
