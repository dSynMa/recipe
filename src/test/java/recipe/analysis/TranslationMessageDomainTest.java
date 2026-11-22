package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationMessageDomainTest {
    private String model(boolean supply,int count) {
        return "channels: c\nmessage-structure: value: 0..1\ncommunication-variables: ready: boolean\n"
            +"agent Sender\nlocal: count: 0..2, sent: boolean\ninit: count == "+count+" & !sent\n"
            +"relabel: ready <- TRUE\nreceive-guard: FALSE\nrepeat: send: <TRUE> "
            +(supply?"SUPPLY@(TRUE)":"c! (TRUE)")+"(value := count + 1)[sent := TRUE]\n"
            +"agent Receiver\nlocal: got: boolean\ninit: !got\nrelabel: ready <- TRUE\nreceive-guard: channel == c\n"
            +"repeat: receive: <value == "+(count+1)+"> "+(supply?"GET@TRUE":"c?")+"[got := TRUE]\n"
            +"system = Sender(sender, TRUE) | Receiver(receiver, TRUE)";
    }
    @Test(timeout=20000)
    public void outOfDomainSendPayloadCannotReachReceiver() throws Exception {
        assertCtl(ToNuXmv.transform(parse(model(false,1))),"EX receiver-got",false);
    }
    @Test(timeout=20000)
    public void outOfDomainSupplyPayloadCannotReachGetter() throws Exception {
        assertCtl(ToNuXmv.transform(parse(model(true,1))),"EX receiver-got",false);
    }
    @Test(timeout=20000)
    public void inDomainPayloadsRemainEnabled() throws Exception {
        for(boolean supply:new boolean[]{false,true}) assertCtl(ToNuXmv.transform(parse(model(supply,0))),"EX receiver-got",true);
    }
}
