import flak.*;
import flak.annotations.Route;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.json.JSONArray;
import org.json.JSONObject;
import org.petitparser.context.ParseError;
import recipe.analysis.NuXmvInteraction;
import recipe.analysis.ToNuXmv;
import recipe.lang.System;
import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.types.Enum;
import recipe.lang.utils.Pair;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.*;

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


    @Route("/buildModel")
    public String buildModel(Request req) {
        Enum.clear();
        String script = req.getQuery().get("script").trim();
        Boolean buildType = Boolean.valueOf(req.getQuery().get("symbolic").trim());
        try {
            system = recipe.lang.System.parser().end().parse(script).get();
            if(nuXmvInteraction != null){
                nuXmvInteraction.stopNuXmvThread();
                nuXmvInteraction = null;
            }
            nuXmvInteraction = new NuXmvInteraction(system);
            Pair<Boolean, String> initialise = nuXmvInteraction.initialise(buildType);
            if(initialise.getLeft()){
                return "{ \"success\" : true}";
            } else{
                return "{ \"error\" : \"" + initialise.getRight() + "\"}";
            }
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

    @Route("/toDOTSVG")
    public String toDOTSVG(Request req) {
        if(system == null){
            return "{ \"error\" : \"Set system by sending your script to /setSystem.\"}";
        }
        try {
            List<String> svgs = new ArrayList<>();

            for(AgentInstance agentInstance : system.getAgentInstances()) {
                Agent agent = agentInstance.getAgent();
                String digraph = "digraph \"" + agentInstance.getLabel() + "\"{\n" + agent.toDOT() + "\n}";
                String name = agent.getName();

                String s = Graphviz.fromString(digraph).engine(Engine.DOT).render(Format.SVG).toString();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", name);
                jsonObject.put("svg", s);
                svgs.add(jsonObject.toString());
            }

            return "[" + String.join(", ", svgs) + "]";
        } catch (ParseError parseError){
            return "{ \"error\" : \"" + parseError.getFailure().toString() + "\"}";
        } catch (Exception e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

    @Route("/modelCheck")
    public String modelCheck(Request req) throws Exception {
        if(system == null){
            return "{ \"error\" : \"Compile system first.\"}";
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

                boolean ic3 = false;
                boolean bounded = false;
                int bound = 10;
                if(req.getQuery().get("bmc") != null && !req.getQuery().get("bmc").equals("")
                        && !req.getQuery().get("bmc").toLowerCase(Locale.ROOT).trim().equals("false")){
                    bounded = true;
                    if(req.getQuery().get("bound") != null && !req.getQuery().get("bound").equals("")){
                        try {
                            bound = Integer.parseInt(req.getQuery().get("bound"));
                        } catch (Exception e){
                        }
                    }
                }
                else if(req.getQuery().get("ic3") != null && !req.getQuery().get("ic3").equals("")
                        && !req.getQuery().get("ic3").toLowerCase(Locale.ROOT).trim().equals("false")){
                    ic3 = true;
                    if(req.getQuery().get("bound") != null && !req.getQuery().get("bound").equals("")){
                        try {
                            bounded = true;
                            bound = Integer.parseInt(req.getQuery().get("bound"));
                        } catch (Exception e){
                        }
                    } else{
                        bound = -1;
                    }
                }

                for(int i = 0; i < system.getLtlspec().size(); i++) {
                    String spec = system.getLtlspec().get(i).replaceAll("LTLSPEC", "").trim();
                    Pair<Boolean, String> result;
                    if(ic3){
                        result = nuXmvInteraction.modelCheckic3(spec, bounded, bound);
                    }
                    else {
                        result = nuXmvInteraction.modelCheck(spec, bounded, bound);
                    }
                    JSONObject resultJSON = new JSONObject();
                    resultJSON.put("spec", spec);

                    if(result.getLeft()) {
                        if(result.getRight().toLowerCase(Locale.ROOT).contains("is false")){
                            resultJSON.put("result", "false");
                        } else if(result.getRight().toLowerCase(Locale.ROOT).contains("is true")){
                            resultJSON.put("result", "true");
                        } else{
                            resultJSON.put("result", "unknown");
                        }
                    } else{
                        resultJSON.put("result", "error");
                    }
                    resultJSON.put("output", result.getRight().replaceAll(" --", "\n--"));
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
            return "{ \"error\" : \"Compile system first.\"}";
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

    Map<String, String> latestDots = new HashMap<>();

    @Route("/simulateNext")
    public String simulateNext(Request req) throws Exception {
        if(system == null){
            return "{ \"error\" : \"Compile system first.\"}";
        }
        if(nuXmvInteraction == null) {
            String outp = init(req);
            if(outp.contains("error")){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", outp);
                return jsonObject.toString();
            }
        }

        String constraint = req.getQuery().get("constraint");

        Pair<Boolean, String> out;

        if(req.getQuery().get("reset").toLowerCase(Locale.ROOT).trim().equals("true")){
            out = nuXmvInteraction.simulation_pick_init_state(constraint);
        } else {
            out = nuXmvInteraction.simulation_next(constraint);
        }

        JSONObject response = nuXmvInteraction.outputToJSON(out.getRight());

        List<JSONObject> svgs = new ArrayList<>();

        for(AgentInstance agentInstance : system.getAgentInstances()) {
            Agent agent = agentInstance.getAgent();
            String name = agentInstance.getLabel();
            String digraph;
            if(latestDots.containsKey(name)){
                digraph = latestDots.get(name);
            } else{
                digraph = "digraph \"" + agentInstance.getLabel() + "\"{\n" + agent.toDOT() + "\n}";
            }

            if(response.has(name) && ((JSONObject) response.get(name)).has("state")){
                String state = ((JSONObject) response.get(name)).get("state").toString();
                digraph = digraph.replaceAll(";[\r\n ]*[^;]+[\r\n ]*\\[color=red\\][\r\n ]*;", ";");
                digraph = digraph.replaceAll("}[ \n\r\t]*", "");
                digraph += state + "[color=red];}";
                latestDots.put(name, digraph);
            }

            String s = Graphviz.fromString(digraph).engine(Engine.DOT).render(Format.SVG).toString();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            jsonObject.put("svg", s);
            svgs.add(jsonObject);
        }

        response.put("svgs", svgs);

        if(out.getLeft()){
            return response.toString();
        } else{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", out.getRight());
            return jsonObject.toString();
        }
    }

    @Route("/resetSimulation")
    public void resetSimulation(Request req) throws Exception {
        nuXmvInteraction = null;
        cors();
    }

    static App app;

    public static int freePort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();

        return port;
    }

    public static String start() throws Exception {
//        int port = freePort();
        int port = 54044;
        app = Flak.createHttpApp(port);
        app.scan(new Server());
        cors();
        app.start();
        return app.getRootUrl();
    }

    public static App app() {
        return app;
    }

    private static void cors(){
        SuccessHandler successHandler = new SuccessHandler() {
            @Override
            public void onSuccess(Request request, Method method, Object[] objects, Object o) {
                app.getResponse().addHeader("Access-Control-Allow-Origin", "*");
                app.getResponse().addHeader("Access-Control-Allow-Methods", "Origin, X-Requested-With, Content-Type, Accept");
            }
        };
        ErrorHandler errorHandler = new ErrorHandler() {
            @Override
            public void onError(int i, Request request, Throwable throwable) {
                app.getResponse().addHeader("Access-Control-Allow-Origin", "*");
                app.getResponse().addHeader("Access-Control-Allow-Methods", "Origin, X-Requested-With, Content-Type, Accept");
            }
        };
        app.addSuccessHandler(successHandler);
        app.addErrorHandler(errorHandler);
    }

    public static void stop(){
        app.stop();
    }

    public static void main(String[] args) throws Exception {
       java.lang.System.out.println("Server started on: " + start());
    }
}
