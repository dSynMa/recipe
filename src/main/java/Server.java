import flak.App;
import flak.Flak;
import flak.Request;
import flak.annotations.Route;
import org.json.JSONObject;
import org.petitparser.context.ParseError;
import recipe.analysis.NuXmvInteraction;
import recipe.analysis.ToNuXmv;
import recipe.lang.System;
import recipe.lang.types.Enum;
import recipe.lang.utils.Pair;

public class Server {
    NuXmvInteraction nuXmvInteraction;
    System system;

    @Route("/")
    public String index() {
        return "Started";
    }

    @Route("/setSystem")
    public String setSystem(Request req) {
        Enum.clear();
        String script = req.getQuery().get("script").trim();
        try {
            system = recipe.lang.System.parser().end().parse(script).get();
            return "{\"symbolic\" : " + system.isSymbolic() + "}";
        } catch (ParseError parseError){
            return "{ \"error\" : \"" + parseError.getFailure().toString() + "\"}";
        } catch (Exception e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

    @Route("/toDOT")
    public String toDOT(Request req) {
        if(system == null){
            return "{ \"error\" : \"Set system by sending your script to \\setSystem.\"}";
        }
        try {
            String toReturn = "{ \"agents\": [" + String.join(",", system.toDOT()) + "]";
            if(system.isSymbolic()){
                toReturn += ", \"symbolic\" : true";
            }
            toReturn += "}";
            return toReturn;
        } catch (ParseError parseError){
            return "{ \"error\" : \"" + parseError.getFailure().toString() + "\"}";
        } catch (Exception e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

    @Route("/modelCheck")
    public String modelCheck(Request req) throws Exception {
        if(system == null){
            return "{ \"error\" : \"Set system by sending your script to \\setSystem.\"}";
        }

        try {
            if(system.getLtlspec() == null || system.getLtlspec().size() == 0){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "No specifications to model check.");
                return jsonObject.toString();
            } else{
                JSONObject jsonObject = new JSONObject();
                String out = ToNuXmv.nuxmvModelChecking(system);
                if(out.equals("")){
                    nuXmvInteraction = new NuXmvInteraction(ToNuXmv.transform(system));
                    for(int i = 0; i < system.getLtlspec().size(); i++) {
                        String spec = system.getLtlspec().get(i).replaceAll("^ *[^ ]+ +", "");
                        Pair<Boolean, String> result = nuXmvInteraction.symbolicModelCheck(spec, 20);
                        if(result.getLeft()) {
                            out += spec + ":\n" + result.getRight() + "\n";
                        } else{
                            out += spec + " (error) :\n" + result.getRight() + "\n";
                        }
                    }
                    nuXmvInteraction.stopNuXmvThread();
                }
                jsonObject.put("result", out);
                return jsonObject.toString();
            }
        } catch (ParseError parseError){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", parseError.getFailure().toString());
            return jsonObject.toString();
        } catch (Exception e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

    @Route("/simulateInit")
    public String simulateInit(Request req) throws Exception {
        if(system == null){
            return "{ \"error\" : \"Set system by sending your script to \\setSystem.\"}";
        }

        try {
            nuXmvInteraction = new NuXmvInteraction(ToNuXmv.transform(system));
            nuXmvInteraction.symbolic = system.isSymbolic();
            Pair<Boolean, String> out = nuXmvInteraction.initialise();
            if(out.getLeft()){
                return NuXmvInteraction.outputToJSON(out.getRight()).toString();
            } else{
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", out.getRight());
                return jsonObject.toString();
            }
        } catch (ParseError parseError){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", parseError.getFailure().toString());
            return jsonObject.toString();
        } catch (Exception e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

//    @Route("/simulatePickState")
//    public String simulatePickState() throws Exception {
//        Pair<Boolean, String> out = nuXmvInteraction.simulation_pick_init_state();
//        if(out.getLeft()){
//            return NuXmvInteraction.outputToJSON(out.getRight()).toString();
//        } else{
//            return new JSONObject("{ error: " + out.getLeft() + "}").toString();
//        }
//    }

    @Route("/simulateNext")
    public String simulateNext(Request req) throws Exception {
        if(system == null){
            return "{ \"error\" : \"Set system by sending your script to \\setSystem.\"}";
        }
        if(nuXmvInteraction == null) {
            String outp = simulateInit(req);
            if(outp.contains("error")){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", outp);
                return jsonObject.toString();            }
        }
        //if keep-all then return stuck
        String sendTransitionLabel = req.getQuery().get("constraint");
        Pair<Boolean, String> out = nuXmvInteraction.simulation_next(sendTransitionLabel);
        if(out.getLeft()){
            return NuXmvInteraction.outputToJSON(out.getRight()).toString();
        } else{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", out.getRight());
            return jsonObject.toString();
        }
    }

    @Route("/resetSimulation")
    public void resetSimulation(Request req) throws Exception {
        nuXmvInteraction = null;
    }

    static App app;

    public static void start(String port) throws Exception {
        app = Flak.createHttpApp(Integer.parseInt(port));
        app.scan(new Server());
        app.start();
    }

    public static void stop(){
        app.stop();
    }
}
