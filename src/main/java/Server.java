import flak.App;
import flak.Flak;
import flak.Request;
import flak.annotations.Route;
import org.json.JSONArray;
import org.json.JSONObject;
import org.petitparser.context.ParseError;
import recipe.analysis.NuXmvInteraction;
import recipe.analysis.ToNuXmv;
import recipe.lang.System;
import recipe.lang.types.Enum;
import recipe.lang.utils.Pair;

import java.util.Locale;

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
            if(nuXmvInteraction != null){
                nuXmvInteraction.stopNuXmvThread();
                nuXmvInteraction = null;
            }
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
            return "{ \"error\" : \"Set system by sending your script to /setSystem.\"}";
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
            return "{ \"error\" : \"Set system by sending your script to /setSystem.\"}";
        }

        if(nuXmvInteraction == null){
            String init = this.init(req);
            if(init.contains("error")){
                return init;
            }
        }

        try {
            if(system.getLtlspec() == null || system.getLtlspec().size() == 0){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "No specifications to model check.");
                return jsonObject.toString();
            } else{
                JSONObject jsonObject = new JSONObject();
                JSONArray array = new JSONArray();

                for(int i = 0; i < system.getLtlspec().size(); i++) {
                    String spec = system.getLtlspec().get(i).replaceAll("LTLSPEC", "").trim();
                    Pair<Boolean, String> result = nuXmvInteraction.modelCheck(spec, 20);
                    JSONObject resultJSON = new JSONObject();
                    resultJSON.put("spec", spec);

                    if(result.getLeft()) {
                        if(result.getRight().toLowerCase(Locale.ROOT).contains("is false")){
                            resultJSON.put("result", "false");
                        } else{
                            resultJSON.put("result", "true");
                        }
                    } else{
                        resultJSON.put("result", "error");
                    }
                    resultJSON.put("output", result.getRight());
                    array.put(resultJSON);
                }
                jsonObject.put("results", array);
                nuXmvInteraction.stopNuXmvThread();
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

    @Route("/init")
    public String init(Request req) throws Exception {
        if(system == null){
            return "{ \"error\" : \"Set system by sending your script to /setSystem.\"}";
        }

        try {
            nuXmvInteraction = new NuXmvInteraction(system);
            return "{}";
        } catch (ParseError parseError){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", parseError.getFailure().toString());
            return jsonObject.toString();
        } catch (Exception e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

    @Route("/simulateNext")
    public String simulateNext(Request req) throws Exception {
        if(system == null){
            return "{ \"error\" : \"Set system by sending your script to /setSystem.\"}";
        }
        if(nuXmvInteraction == null) {
            String outp = init(req);
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

    public static App app() {
        return app;
    }

    public static void stop(){
        app.stop();
    }

    public static void main(String[] args) throws Exception {
        app = Flak.createHttpApp(Integer.parseInt(args[0]));
        app.scan(new Server());
        app.start();
    }
}
