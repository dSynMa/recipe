
import flak.*;
import flak.annotations.Route;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.petitparser.context.ParseError;

import recipe.analysis.NuXmvBatch;
import recipe.analysis.NuXmvInteraction;
import recipe.analysis.ToNuXmv;
import recipe.interpreter.Interpreter;
import recipe.lang.System;
import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.ltol.LTOL;
import recipe.lang.ltol.Observation;
import recipe.lang.types.Enum;
import recipe.lang.utils.Pair;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class Server {
    NuXmvInteraction nuXmvInteraction;
    System system;
    Map<String, Observation> obsMap;

    Interpreter interpreter;
    Map<String, String> latestDots = new HashMap<>();
    Map<String, String> latestDotsInterpreter = new ConcurrentHashMap<>();

    MCConfig mcConfig;

    private static Logger logger = Logger.getLogger(Server.class.getName());
    

    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // TODO can we use multiple nuXmvInteraction and parallelize this?
    private final ExecutorService mcService = Executors.newFixedThreadPool(1);
    private Semaphore systemSem = new Semaphore(1);

    private List<JSONObject> renderSVGs (JSONObject response) {
        List<JSONObject> svgs = new ArrayList<>();
        List<Future<JSONObject>> futures = new ArrayList<>(system.getAgentInstances().size());

        for(AgentInstance agentInstance : system.getAgentInstances()) {
            futures.add(this.service.submit(new SVGWorker(agentInstance, response)));
        }
        try {
            for(Future<JSONObject> f : futures) {
                svgs.add(f.get());
            }
        } catch (Exception e) {
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            svgs.clear();
            svgs.add(err);
        }
        return svgs;
    }

    private class SVGWorker implements Callable<JSONObject> {
        private AgentInstance agentInstance;
        private JSONObject response;

        public SVGWorker(AgentInstance instance, JSONObject response) {
            this.agentInstance = instance;
            this.response = response;
        }
        @Override
        public JSONObject call() throws Exception {
            return renderSVG();
        }

        private JSONObject renderSVG () {
            Agent agent = agentInstance.getAgent();
            String name = agentInstance.getLabel();
            String digraph;
            if(latestDotsInterpreter.containsKey(name)){
                digraph = latestDotsInterpreter.get(name);
            } else{
                digraph = "digraph \"" + agentInstance.getLabel() + "\"{\n" + agent.toDOT() + "\n}";
            }

            String query = String.format("/state/%s/**state**", name);
            String queryTransition = String.format("/state/%s/**last_transition**", name);
            
            Object stateQueryResult = response.query(query);
            Object trQueryResult = response.query(queryTransition);

            if(stateQueryResult != null){
                String state = stateQueryResult.toString();
                
                // Remove closing brace and past highlighting
                digraph = digraph.substring(0, digraph.length()-1);
                digraph = digraph.replaceAll(";[\r\n ]*[^;]+[\r\n ]*\\[color=red\\][\r\n ]*;", ";");
                digraph = digraph.replace("width=1,color=red", "width=1");
                
                // Highlight current state
                digraph += state + "[color=red];";
            
                // Highlight last transition
                if (trQueryResult != null) {
                    String queryFromState = String.format("/state/%s/**from_state**", name);
                    String queryLbl = String.format("/state/%s/**last_label**", name);
                    String fromState = response.query(queryFromState).toString();
                    String lbl = response.query(queryLbl).toString();
                    String lastTr = trQueryResult.toString();
                    
                    
                    String newTr = String.format("%s -> %s[label=\"%s\",labeltooltip=\"%s\",width=1];", fromState, state, lbl, lastTr);
                    
                    digraph = digraph.replace(newTr, "\n");
                    digraph += newTr.replace("];", ",color=red];");
                }
                digraph += "}";
                latestDotsInterpreter.put(name, digraph);
            }

            String s = Graphviz.fromString(digraph).engine(Engine.DOT).render(Format.SVG).toString();
            s = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n%s", s);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            jsonObject.put("svg", s);
            return jsonObject;
        }
    }

    private class MCWorker implements Callable<JSONObject> {
        private int i;

        public MCWorker(int i) {
            this.i = i;
        }
        public JSONObject call() throws Exception {
            return doModelCheck(i);
        }
    }


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
        interpreter = null;
        latestDots = new HashMap<>();
        latestDotsInterpreter = new ConcurrentHashMap<>();

        String script = req.getQuery().get("script").trim();
        Boolean buildType = Boolean.valueOf(req.getQuery().get("symbolic").trim());
        JSONObject response = new JSONObject();
        try {
            system = recipe.lang.System.parser().end().parse(script).get();
            if(nuXmvInteraction != null){
                nuXmvInteraction.stopNuXmvThread();
                nuXmvInteraction = null;
            }
            nuXmvInteraction = new NuXmvInteraction(system);
            Pair<Boolean, String> initialise = nuXmvInteraction.initialise(buildType);

            if(initialise.getLeft()){
                response.put("success", true);
                // return "{ \"success\" : true}";
            } else{
                response.put("error", initialise.getRight());
            }
        } catch (ParseError parseError){
            response.clear();
            response.put("error", parseError.getFailure());
            logger.info(response.toString());
        } catch (Exception e) {
            response.clear();
            response.put("error", e.getMessage() );
            logger.info(response.toString());
        }
        return response.toString();
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

    enum MCType { IC3, BMC, BDD }

    public static class MCConfig {
        private MCType type;
        private int bound;

        public MCType getType() { return type; }
        public int getBound() { return bound; }
        public boolean isBounded() { return bound > -1; }
        // public MCConfig() { this.type = MCType.BDD; this.bound = -1; }
        public MCConfig(MCType type, int bound) { this.type = type; this.bound = bound; }

        public static MCConfig ofRequest(Request req) {
            MCType type = MCType.BDD;
            int bound = -1;
            if(req.getQuery().get("bmc") != null && !req.getQuery().get("bmc").equals("")
                    && !req.getQuery().get("bmc").toLowerCase(Locale.ROOT).trim().equals("false")){
                type = MCType.BMC;
                if(req.getQuery().get("bound") != null && !req.getQuery().get("bound").equals("")){
                    bound = Integer.parseInt(req.getQuery().get("bound"));
                }
            }
            else if(req.getQuery().get("ic3") != null && !req.getQuery().get("ic3").equals("")
                    && !req.getQuery().get("ic3").toLowerCase(Locale.ROOT).trim().equals("false")){
                type = MCType.IC3;
                if(req.getQuery().get("bound") != null && !req.getQuery().get("bound").equals("")){
                    bound = Integer.parseInt(req.getQuery().get("bound"));
                } else{
                    bound = -1;
                }
            }
            return new MCConfig(type, bound);
        }
    }


    private JSONObject doModelCheck(int i) {
        JSONObject resultJSON = new JSONObject();
        Pair<Boolean, String> result;
        try {
            systemSem.acquire();

            // Prune other ltol formulas
            List<LTOL> oldSpecs = system.getSpecs();
            LTOL ltol = oldSpecs.get(i);
            List<LTOL> singleton = new ArrayList<>(1);
            singleton.add(ltol);
            system.setSpecs(singleton);
            
            Pair<List<LTOL>,Map<String, Observation>> toLtl = ToNuXmv.ltolToLTLAndObservationVariables(system.getSpecs());
            List<LTOL> specs = toLtl.getLeft();
            obsMap = toLtl.getRight();
            String spec = specs.get(0).toString();
            String unparsedSpec = system.getUnparsedSpecs().get(i).trim();

            String info = String.format("[%d]  %s, %s", i, unparsedSpec, mcConfig.type);
            logger.info(info);
            switch (mcConfig.getType()) {
                case BDD:
                    NuXmvInteraction nuxmv = new NuXmvInteraction(system);
                    nuxmv.initialise(true);
                    nuxmv.initialise(mcConfig.getType() == MCType.BMC);
                    result = nuxmv.modelCheck(spec, mcConfig.isBounded(), mcConfig.getBound());
                    // Stop NuXmv
                    nuxmv.stopNuXmvThread();
                    break;
                default:
                    NuXmvBatch n = new NuXmvBatch(system);
                    result = n.modelCheck(spec, mcConfig.isBounded(), mcConfig.getBound(), mcConfig.getType() == MCType.BMC);
                    break;
            }

            // Restore all formulas
            system.setSpecs(oldSpecs);
            systemSem.release();

            resultJSON.put("spec", unparsedSpec);

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

            String output = result.getRight().replaceAll(" --", "\n--");
            resultJSON.put("output", output);
        } catch (Exception e) {
            resultJSON.clear();
            e.printStackTrace();
            resultJSON.put("error", e.getMessage());
        }
        return resultJSON;
    }

    @Route("/modelCheck/:id")
    public String modelCheck(Request req, String idString) {
        JSONObject result;
        String info = "";
        try {
            int id = Integer.valueOf(idString);
            // MCConfig config = MCConfig.ofRequest(req);
            Future<JSONObject> future = this.mcService.submit(new MCWorker(id));
            result = future.get();
            info = String.format("[%s] -- DONE", idString);
        } catch (Exception e) {
            e.printStackTrace();
            info = String.format("[%s] -- ERROR", idString);
            result = new JSONObject();
            result.put("error", e.getMessage());
        } finally {
            logger.info(info);
        }
        return result.toString();
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
            if(system.getSpecs() == null || system.getSpecs().size() == 0){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "No specifications to model check.");
                return jsonObject.toString();
            } else{
                JSONObject jsonObject = new JSONObject();
                JSONArray array = new JSONArray();
                mcConfig = MCConfig.ofRequest(req);

                for(int i = 0; i < system.getUnparsedSpecs().size(); i++) {
                    JSONObject jo = new JSONObject();
                    jo.put("id", i);
                    jo.put("spec", system.getUnparsedSpecs().get(i));
                    jo.put("url", String.format("/modelCheck/%d", i));
                    array.put(jo);
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

    @Route("/modelCheckStop")
    public String stopModelChecking(Request req) {
        NuXmvBatch.stopAllProcesses();
        return "";
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

    @Route("/interpretNext")
    public String interpretNext(Request req) throws Exception {
        if (system == null) {
            return "{ \"error\" : \"Compile system first.\"}";
        }
        if (interpreter == null) {
            interpreter = new Interpreter(system);
        }

        if(req.getQuery().get("reset").toLowerCase(Locale.ROOT).trim().equals("true")){
            interpreter = new Interpreter(system);
            interpreter.init("TRUE");
        } else {
            int index = Integer.parseInt(req.getQuery().get("index"));
            if (interpreter.isDeadlocked()) {
                return "{ \"error\" : \"No successor state (system is deadlocked).\"}";
            }
            interpreter.next(index);
        }
        JSONObject response = interpreter.getCurrentStep().toJSON();
        response.put("svgs", renderSVGs(response));

        return response.toString();
    }

    @Route("/interpretBack")
    public String interpretBack(Request req) throws Exception {
        interpreter.backtrack();
        JSONObject response = interpreter.getCurrentStep().toJSON();
        response.put("svgs", renderSVGs(response));
        return response.toString();
    }

    @Route("/resetInterpreter")
    public void resetInterpreter(Request req) throws Exception {
        interpreter = new Interpreter(system);
        cors();
    }

    @Route("/interpretLoad")
    public String loadInterpreter(Request req) {
        // Load trace into interpreter
        String output = req.getQuery().get("output");
        JSONObject response = new JSONObject();

        try {
            interpreter = Interpreter.ofTrace(system, obsMap, output);
            List<JSONObject> trace = interpreter.traceToJSON();
            response.put("svgs", renderSVGs(trace.get(trace.size()-1)));
            response.put("trace", trace);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
        }
        return response.toString();
    }


    @Route("/interpretLoadJSON")
    public String loadInterpreterJSON(Request req) {
        String trace = req.getQuery().get("trace");
        JSONTokener toks = new JSONTokener(trace);
        JSONArray json = new JSONArray(toks);

        try {
            interpreter = Interpreter.ofJSON(system, obsMap, json);
            return "{}";
        } catch (Exception e) {
            return String.format("{ \"error\" : \"%s\"}", e.getMessage());
        }
    }

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
       logger.info("Server started on: " + start());
    }
}
