
import flak.*;
import flak.annotations.Route;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.petitparser.context.ParseError;

import recipe.RCheckInterop;
import recipe.analysis.NuXmvInteraction;
import recipe.analysis.NuXmvModelFile;
import recipe.analysis.ToNuXmv;
import recipe.interpreter.Interpreter;
import recipe.lang.System;
import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.ltol.LTOL;
import recipe.lang.types.Enum;
import recipe.lang.utils.Pair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.CancellationException;
import java.util.logging.Logger;

public class Server {
    NuXmvInteraction nuXmvInteraction;
    private boolean jsonInput;

    // Accessed under the server monitor. Only the matching request can cancel a build.
    private static class Build {
        final String id = UUID.randomUUID().toString();
        Thread worker;
        boolean started;
        boolean cancelled;
    }

    private Build build;
    private final Set<Future<?>> modelCheckWorkers = ConcurrentHashMap.newKeySet();
    private NuXmvModelFile checkModel;
    private List<String> checkFormulas;
    System system;

    Interpreter interpreter;
    Map<String, String> latestDots = new HashMap<>();
    Map<String, String> latestDotsInterpreter = new ConcurrentHashMap<>();

    volatile MCConfig mcConfig;

    private static Logger logger = Logger.getLogger(Server.class.getName());
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

    private NuXmvModelFile modelFile() throws Exception {
        if (checkModel == null) checkModel = new NuXmvModelFile(system);
        return checkModel;
    }

    private void replaceSystem(System replacement) throws IOException {
        cancelChecks();
        resetChecker();
        if (checkModel != null) checkModel.close();
        checkModel = null;
        checkFormulas = null;
        system = replacement;
    }

    private void resetChecker() throws IOException {
        if (nuXmvInteraction != null) {
            nuXmvInteraction.close();
            nuXmvInteraction = null;
        }
    }

    /**
     * Parse a script sent by the GUI through the JavaScript parser CLI, so the
     * server and the editor use the same grammar and deserializer.
     */
    private recipe.lang.System parseSystem(String script) throws Exception {
        if (!jsonInput) {
            return recipe.lang.System.parser().end().parse(script).get();
        }
        String decoded = script.trim();
        try {
            decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw e;
        }
        return RCheckInterop.parseAndDeserialize(decoded);
    }

    @Route("/")
    public String index() {
        return "Started";
    }

    private synchronized void discardCurrentSystem() throws IOException {
        replaceSystem(null);
        Enum.clear();
        interpreter = null;
        mcConfig = null;
        latestDots.clear();
        latestDotsInterpreter.clear();
    }

    @Route("/setSystem")
    public synchronized String setSystem(Request req) {
        if (build != null && build.worker != null) return buildError("A model is building");
        build = null;
        try {
            discardCurrentSystem();
            String script = req.getQuery().get("script").trim();
            system = parseSystem(script);
            return "{\"symbolic\" : " + system.isSymbolic() + "}";
        } catch (ParseError parseError){
            return "{ \"error\" : \"" + parseError.getFailure().toString() + "\"}";
        } catch (Exception e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }


    private static String buildError(String message) {
        return new JSONObject().put("error", message).toString();
    }

    @Route("/prepareBuild")
    public synchronized String prepareBuild(Request req) {
        if (build != null && build.worker != null) return buildError("A model is building");
        build = new Build();
        return new JSONObject().put("buildId", build.id).toString();
    }

    @Route("/cancelBuild")
    public synchronized String cancelBuild(Request req) throws IOException {
        if (build != null && build.id.equals(req.getQuery().get("buildId"))) {
            build.cancelled = true;
            if (build.worker != null) build.worker.interrupt();
            else if (build.started) discardCurrentSystem(); // Completion raced with Cancel.
        }
        return "{}";
    }

    @Route("/buildModel")
    public String buildModel(Request req) {
        String id = req.getQuery().get("buildId");
        Build job;
        synchronized (this) {
            if (build != null && build.worker != null) return buildError("A model is building");
            // Older clients can still build directly without reserving an ID.
            if (id == null) build = new Build();
            if (build == null || (id != null && !build.id.equals(id)) || build.cancelled || build.started)
                return buildError("This build is no longer pending");
            job = build;
            job.started = true;
            job.worker = Thread.currentThread();
        }

        NuXmvModelFile builtModel = null;
        NuXmvInteraction checker = null;
        try {
            discardCurrentSystem();
            String script = req.getQuery().get("script").trim();
            boolean symbolic = Boolean.parseBoolean(req.getQuery().get("symbolic").trim());
            System model = parseSystem(script);
            if (Thread.currentThread().isInterrupted()) return buildError("Build cancelled");
            builtModel = new NuXmvModelFile(model);
            checker = builtModel.openChecker();
            Pair<Boolean, String> result = checker.initialise(symbolic);
            synchronized (this) {
                if (job.cancelled) return buildError("Build cancelled");
                if (!result.getLeft()) return buildError(result.getRight());
                system = model;
                checkModel = builtModel;
                builtModel = null;
                nuXmvInteraction = checker;
                checker = null; // Ownership transfers to the active model.
                job.worker = null;
                return new JSONObject().put("success", true).toString();
            }
        } catch (Exception e) {
            return buildError(Objects.toString(e.getMessage(), e.toString()));
        } finally {
            try {
                try {
                    if (checker != null) checker.close();
                } finally {
                    if (builtModel != null) builtModel.close();
                }
            } catch (IOException cleanupFailure) {
                logger.warning(cleanupFailure.toString());
            }
            synchronized (this) {
                if (job.worker != null && job.cancelled) Thread.interrupted();
                job.worker = null;
            }
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


    private JSONObject doModelCheck(NuXmvModelFile model, MCConfig mcConfig, String spec, String unparsedSpec) {
        JSONObject resultJSON = new JSONObject();
        Pair<Boolean, String> result;
        try {
            // Each worker starts its own process against the build's shared translation.
            try (NuXmvInteraction checker = model.openChecker()) {
                switch (mcConfig.getType()) {
                    case BDD:
                        result = checker.modelCheck(spec, false, mcConfig.getBound());
                        break;
                    case BMC:
                        result = checker.modelCheckBmc(spec, mcConfig.isBounded(), mcConfig.getBound());
                        break;
                    default:
                        result = checker.modelCheckIc3(spec, mcConfig.isBounded(), mcConfig.getBound());
                        break;
                }
            }

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

    private synchronized FutureTask<JSONObject> submitCheck(int id) throws Exception {
        MCConfig config = mcConfig;
        if (config == null) throw new IllegalStateException("No active model check");
        NuXmvModelFile model = modelFile();
        if (checkFormulas == null) {
            // Number observations across the full list, as in model generation.
            List<String> formulas = new ArrayList<>();
            for (LTOL formula : ToNuXmv.ltolToLTLAndObservationVariables(system.getSpecs()).getLeft()) {
                formulas.add(formula.toNuXmv());
            }
            checkFormulas = formulas;
        }
        String formula = checkFormulas.get(id);
        String source = system.getUnparsedSpecs().get(id).trim();
        FutureTask<JSONObject> task = new FutureTask<>(() -> doModelCheck(model, config, formula, source));
        synchronized (modelCheckWorkers) {
            if (config != mcConfig) throw new CancellationException();
            modelCheckWorkers.add(task);
            try {
                service.execute(task);
            } catch (RuntimeException error) {
                modelCheckWorkers.remove(task);
                throw error;
            }
        }
        return task;
    }

    @Route("/modelCheck/:id")
    public String modelCheck(Request req, String idString) {
        FutureTask<JSONObject> task = null;
        try {
            task = submitCheck(Integer.parseInt(idString));
            return task.get().toString();
        } catch (CancellationException cancelled) {
            return new JSONObject().put("error", "Model check cancelled").toString();
        } catch (InterruptedException interrupted) {
            if (task != null) task.cancel(true);
            Thread.currentThread().interrupt();
            return new JSONObject().put("error", "Model check interrupted").toString();
        } catch (Exception error) {
            return new JSONObject().put("error", error.toString()).toString();
        } finally {
            if (task != null) modelCheckWorkers.remove(task);
        }
    }

    @Route("/modelCheck")
    public synchronized String modelCheck(Request req) throws Exception {
        if(system == null){
            return "{ \"error\" : \"Compile system first.\"}";
        }

        try {
            if(system.getSpecs() == null || system.getSpecs().size() == 0){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "No specifications to model check.");
                return jsonObject.toString();
            } else{
                JSONObject jsonObject = new JSONObject();
                JSONArray array = new JSONArray();
                modelFile();
                mcConfig = MCConfig.ofRequest(req);

                for(int i = 0; i < system.getUnparsedSpecs().size(); i++) {
                    JSONObject jo = new JSONObject();
                    jo.put("id", i);
                    jo.put("spec", system.getUnparsedSpecs().get(i));
                    jo.put("url", String.format("/modelCheck/%d", i));
                    array.put(jo);
                }
                jsonObject.put("results", array);
                resetChecker();

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
        cancelChecks();
        return "";
    }

    private void cancelChecks() {
        synchronized (modelCheckWorkers) {
            mcConfig = null;
            for (Future<?> worker : modelCheckWorkers) worker.cancel(true);
        }
    }

    @Route("/init")
    public synchronized String init(Request req) throws Exception {
        if(system == null){
            return "{ \"error\" : \"Compile system first.\"}";
        }

        try {
            resetChecker();
            nuXmvInteraction = modelFile().openChecker();
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
        try {
            if (interpreter == null || req.getQuery().get("reset").equalsIgnoreCase("true")) {
                Interpreter replacement = new Interpreter(system);
                replacement.init("TRUE");
                interpreter = replacement;
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
        } catch (Exception failure) {
            return new JSONObject().put("error", failure.getMessage()).toString();
        }
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
            interpreter = Interpreter.ofTrace(system, output);
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
        try {
            JSONTokener toks = new JSONTokener(trace);
            JSONArray json = new JSONArray(toks);
            interpreter = Interpreter.ofJSON(system, json);
            return "{}";
        } catch (Exception e) {
            return new JSONObject().put("error", e.getMessage()).toString();
        }
    }

    @Route("/simulateNext")
    public synchronized String simulateNext(Request req) throws Exception {
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
    public synchronized void resetSimulation(Request req) throws Exception {
        resetChecker();
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
        return start(1, 54044, null);
    }

    public static String start(int threads) throws Exception {
        return start(threads, 54044, null);
    }

    public static String start(int threads, Path jsonPath) throws Exception {
        return start(threads, 54044, jsonPath);
    }

    /**
     * Start the API server, optionally loading the system from a JSON model
     * produced by the editor.
     *
     * <p>A port of 0 selects a free port and prints {@code PORT: <port>} to
     * stderr so the extension can connect to it.
     */
    public static String start(int threads, int port, Path jsonPath) throws Exception {
        if (port == 0) {
            port = freePort();
        }
        java.lang.System.err.println("PORT: " + port);
        app = Flak.createHttpApp(port);
        Server server = new Server();
        if (jsonPath != null) {
            JSONObject jo = RCheckInterop.parseJson(jsonPath);
            server.system = recipe.lang.System.deserialize(jo);
            server.jsonInput = true;
        }
        app.scan(server);
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
