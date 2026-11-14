package recipe.analysis;

import java.math.BigDecimal;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayScientificNumberTest {
    private void check(Object value, String decimal) throws Exception {
        var model=parse(source("arithmetic-multiplication").replace("count: integer","count: real")
            .replace("count == 2","count == " + decimal).replace("count * 2","count"));
        JSONObject initial=new JSONObject("{sender:{sent:false},receiver:{received:false}}");
        initial.getJSONObject("sender").put("count",value);
        JSONObject next=new JSONObject("{sender:{sent:true},receiver:{received:true}}");
        assertEquals(2,Interpreter.ofJSON(model,new JSONArray().put(initial).put(next)).traceToJSON().size());
    }
    @Test(timeout=20000)
    public void nativeSmallJsonNumberReplays() throws Exception {check(new BigDecimal("1E-8"),"0.00000001");}
    @Test(timeout=20000)
    public void nativeLargeJsonNumberReplays() throws Exception {check(new BigDecimal("1E+20"),"100000000000000000000.0");}
    @Test(timeout=20000)
    public void decimalStringControlReplays() throws Exception {check("0.00000001","0.00000001");}
    @Test(timeout=20000)
    public void nativeNegativeSmallJsonNumberReplays() throws Exception {
        check(new BigDecimal("-1E-8"), "(0 - 0.00000001)");
    }
}
