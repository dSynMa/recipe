import flak.App;
import flak.Flak;
import flak.Request;
import flak.annotations.Route;
import org.json.JSONObject;
import org.petitparser.context.ParseError;
import recipe.analysis.NuXmvSimulation;
import recipe.analysis.ToNuXmv;
import recipe.lang.System;
import recipe.lang.types.Enum;
import recipe.lang.utils.Pair;

import java.awt.*;
import java.net.URI;

public class Server {
    NuXmvSimulation nuXmvSimulation;

    @Route("/")
    public String index() {
        return "Started";
    }

    @Route("/toDOT")
    public String toDOT(Request req) {
        Enum.clear();
        String script = req.getQuery().get("script").trim();
        try {
            recipe.lang.System system = recipe.lang.System.parser().end().parse(script).get();
            String toReturn = "{ \"agents\": [" + String.join(",", system.toDOT()) + "]}";
            return toReturn;
        } catch (ParseError parseError){
            return "{ \"error\" : \"" + parseError.getFailure().toString() + "\"}";
        } catch (Exception e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

    @Route("/modelCheck")
    public String modelCheck(Request req) throws Exception {
        String script = req.getQuery().get("script");
        try {
            recipe.lang.System system = recipe.lang.System.parser().parse(script).get();
            return ToNuXmv.nuxmvModelChecking(system);
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
        String script = req.getQuery().get("script");

        try {
            nuXmvSimulation = new NuXmvSimulation(ToNuXmv.transform(System.parser().end().parse(script).get()));
            Pair<Boolean, String> out = nuXmvSimulation.initialise();
            if(out.getLeft()){
                return NuXmvSimulation.outputToJSON(out.getRight()).toString();
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
//        Pair<Boolean, String> out = nuXmvSimulation.simulation_pick_init_state();
//        if(out.getLeft()){
//            return NuXmvSimulation.outputToJSON(out.getRight()).toString();
//        } else{
//            return new JSONObject("{ error: " + out.getLeft() + "}").toString();
//        }
//    }

    @Route("/simulateNext")
    public String simulateNext(Request req) throws Exception {
        //if keep-all then return stuck
        String sendTransitionLabel = req.getQuery().get("constraint");
        Pair<Boolean, String> out = nuXmvSimulation.simulation_next(sendTransitionLabel);
        if(out.getLeft()){
            return NuXmvSimulation.outputToJSON(out.getRight()).toString();
        } else{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", out.getRight());
            return jsonObject.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        App app = Flak.createHttpApp(Integer.parseInt(args[0]));
        app.scan(new Server());
        app.start();
    }
}
