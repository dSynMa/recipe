package recipe.analysis;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.HashSet;
import java.util.Set;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.json.JSONObject;

import recipe.Config;
import recipe.lang.utils.Pair;

public class NuXmvInteraction implements AutoCloseable {

    private static final Pattern PROMPT = Pattern.compile("(?i)nuXmv *>\\s*$");
    // Empty responses signal shutdown or EOF; a complete response always includes a prompt.
    private final BlockingQueue<String> responses = new LinkedBlockingQueue<>();
    private volatile IOException responseFailure;
    private Process checkerProcess;
    private OutputStream commandInput;
    private Thread shutdownHook;

    Thread process;
    private volatile boolean stopped;
    boolean go = false;
    boolean go_msat = false;
    private boolean booleanModelBuilt;
    private boolean booleanModelUnavailable;
    boolean simulationStarted = false;
    private String simulationStateHeader;
    Path path;
    recipe.lang.System system;

    public NuXmvInteraction(recipe.lang.System system) throws Exception {
        this.system = system;
        String nuxmvScript = ToNuXmv.transform(system);
        path = Files.createTempFile("rcheck-interactive-", ".smv");
        path.toFile().deleteOnExit();
        Files.write(path, nuxmvScript.getBytes(StandardCharsets.UTF_8));
        try {
            process = startNuXmvThread();
        } catch (IOException startupFailure) {
            try {
                if (checkerProcess != null) stopNuXmvThread();
                else Files.deleteIfExists(path);
            } catch (IOException cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            throw startupFailure;
        }
    }

    public synchronized String execute(Predicate<String> isFinished, String command) throws IOException {
        ensureRunning();
        commandInput.write((command + "\n").getBytes(StandardCharsets.UTF_8));
        commandInput.flush();

        StringBuilder output = new StringBuilder();
        while (true) {
            output.append(awaitResponse());
            String text = output.toString();
            if (text.contains("not built") || isFinished.test(text)) return text;
        }
    }

    private String awaitResponse() throws IOException {
        ensureRunning();
        try {
            String response = responses.take();
            if (response.isEmpty()) {
                responses.offer(response);
                throw responseFailure != null ? responseFailure : new IOException("nuXmv interaction has stopped");
            }
            return response;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for nuXmv", interrupted);
        }
    }

    public Pair<Boolean, String> modelCheck(String property, Boolean bmc, int steps) throws IOException {
        if (bmc) return modelCheckBmc(property, true, steps);
        if (system.isSymbolic() || booleanModelUnavailable) return modelCheckSmt(property, steps);
        Pair<Boolean, String> initialised = initialise(false);
        if (!initialised.getLeft()) {
            if (!booleanEncodingFailed(initialised.getRight())) return initialised;
            booleanModelUnavailable = true;
            return modelCheckSmt(property, steps);
        }
        Pair<Boolean, String> result = check("check_ltlspec -p \"" + property + "\"");
        return booleanEncodingFailed(result.getRight()) ? modelCheckSmt(property, steps) : result;
    }

    private Pair<Boolean, String> modelCheckSmt(String property, int steps) throws IOException {
        if (steps >= 0) return modelCheckBmc(property, true, steps);
        Pair<Boolean, String> initialised = initialise(true);
        if (!initialised.getLeft()) return initialised;
        return check("check_ltlspec_ic3 -i -p \"" + property + "\"");
    }

    public Pair<Boolean, String> modelCheckBmc(String property, boolean bounded, int steps) throws IOException {
        Pair<Boolean, String> initialised = initialise(true);
        if (!initialised.getLeft()) return initialised;
        // Completeness checking permits a proof; omitting -k would silently stop at 10.
        String command = bounded ? "msat_check_ltlspec_bmc -k " + steps
                : "msat_check_ltlspec_sbmc_inc -c -k " + Integer.MAX_VALUE;
        return check(command + " -p \"" + property + "\"");
    }

    public Pair<Boolean, String> modelCheckIc3(String property, boolean bounded, int steps) throws IOException {
        Pair<Boolean, String> initialised = initialise(true);
        if (!initialised.getLeft()) return initialised;
        // nuXmv 2.0's finite IC3 engine does not enforce -k; use SMT for frame limits.
        boolean useBooleanModel = !bounded && !system.isSymbolic() && !booleanModelUnavailable;
        if (useBooleanModel && !booleanModelBuilt) {
            String output = execute(text -> text.trim().endsWith("nuXmv >"), "build_boolean_model");
            if (booleanEncodingFailed(output)) {
                booleanModelUnavailable = true;
                useBooleanModel = false;
            } else {
                if (hasCheckerError(output)) return new Pair<>(false, output);
                booleanModelBuilt = true;
            }
        }
        Pair<Boolean, String> result = check("check_ltlspec_ic3" + (useBooleanModel ? "" : " -i")
                + (bounded ? " -k " + steps : "") + " -p \"" + property + "\"");
        // A finite model can still have a property containing real arithmetic.
        return useBooleanModel && booleanEncodingFailed(result.getRight())
                ? modelCheckSmt(property, -1) : result;
    }

    private static boolean booleanEncodingFailed(String output) {
        return output.contains("Impossible to build a boolean FSM with infinite precision variables")
                || output.contains("not suitable constants for operator")
                // nuXmv 2.0 reports unsupported FLOOR nodes as a Boolean encoder error.
                || output.contains("bdd_enc_eval_recur:");
    }

    private Pair<Boolean, String> check(String command) throws IOException {
        String output = execute(text -> text.trim().endsWith("nuXmv >"), command);
        output = output.replace("nuXmv > ", "").trim();
        return new Pair<>(!hasCheckerError(output) && hasCheckResult(output), output);
    }

    private static boolean hasCheckerError(String output) {
        // A command may print a verdict before failing while constructing its trace.
        return Pattern.compile("(?m)^\\s*file .+: line [0-9]+:").matcher(output).find()
                || output.contains("*** internal error ***")
                || output.contains("Type System Violation")
                || output.contains("syntax error")
                || output.contains("Parsing error")
                || output.contains("There is a division by 0")
                || output.contains("cannot be converted into INVARSPEC")
                || output.contains("Impossible to build a boolean FSM with infinite precision variables")
                || output.contains("aborting 'source");
    }

    private static boolean hasCheckResult(String output) {
        return output.lines().anyMatch(line -> line.startsWith("-- ") &&
                (line.endsWith(" is true") || line.endsWith(" is false")
                || line.startsWith("-- no counterexample found with bound ")
                || line.startsWith("-- no proof or counterexample found with bound ")));
    }

    public Pair<Boolean, String> initialise(boolean simulateOrBMC) throws IOException {
        ensureRunning();
        boolean smt = system.isSymbolic() || booleanModelUnavailable || simulateOrBMC;
        if (smt ? go_msat : go) return new Pair<>(true, "");
        String out = execute(x -> x.trim().endsWith("nuXmv >"), smt ? "go_msat" : "go");
        // SMT simulation supports real arithmetic directly; Boolean conversion can
        // reject valid real expressions even when every state variable is finite.

        if (hasCheckerError(out)) {
            return new Pair<>(false, out);
        }

        if (smt) go_msat = true;
        else go = true;
        return new Pair<>(true, out);
    }

    public Pair<Boolean, String> simulation_pick_init_state(String constraint) throws IOException {
        if(!go_msat){
            Pair<Boolean, String> initialise = initialise(true);
            if(!initialise.getLeft()){
                return initialise;
            }
        }
        String out = execute((x) -> !x.trim().toLowerCase(Locale.ROOT).replaceAll("( |nuxmv *>)", "").equals("") && x.trim().endsWith("nuXmv >"), "msat_pick_state -v -c \"" + constraint + "\"");
        if(out.contains(" file ") || out.contains("No trace")){
            return new Pair<>(false, out);
        }

        String state = parseLastState(out);
        if (!state.startsWith("State:")) return new Pair<>(false, out);
        simulationStateHeader = state.lines().findFirst().orElse("");
        simulationStarted = true;
        return new Pair<>(true, state);
    }

    private String parseLastState(String text){
        String[] states = text.split("\\->");
        if(states.length <= 1){
            return "";
        } else{
            return states[states.length - 1].replaceAll("nuXmv >", "").trim();
        }
    }

    public Pair<Boolean, String> simulation_next(String constraint) throws IOException {
        if(!go_msat){
            Pair<Boolean, String> initialise = initialise(true);
            if(!initialise.getLeft()){
                return initialise;
            }
        }
        if(!simulationStarted) return simulation_pick_init_state(constraint);

        String out = execute((x) -> !x.trim().toLowerCase(Locale.ROOT).replaceAll("( |nuxmv *>)", "").equals("") && x.trim().endsWith("nuXmv >"), "msat_simulate -k 1 -v -c \"" + constraint + "\"").replaceAll("(nuXmv >)", "").trim();
        if (out.contains("UNSAT")) {
            return new Pair<>(false, "No reachable states.");
        } else {
            String state = parseLastState(out);
            if (!state.startsWith("State:")) return new Pair<>(false, out);
            String header = state.lines().findFirst().orElse("");
            // nuXmv can print the existing trace without an UNSAT diagnostic when
            // the constraint admits no successor. A state must actually advance.
            if (header.equals(simulationStateHeader)) return new Pair<>(false, "No reachable states.");
            simulationStateHeader = header;
            return new Pair<>(true, state);
        }
    }

    private void ensureRunning() throws IOException {
        if (stopped || Thread.currentThread().isInterrupted()) {
            throw new IOException("nuXmv interaction has stopped");
        }
        if (responseFailure != null) throw responseFailure;
    }

    public Thread startNuXmvThread() throws IOException {
        checkerProcess = new ProcessBuilder(Config.getNuxmvPath(), "-int", path.toString())
                .redirectErrorStream(true).start();
        commandInput = checkerProcess.getOutputStream();
        shutdownHook = new Thread(checkerProcess::destroyForcibly);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        process = new Thread(this::readResponses, "nuXmv-output");
        process.setDaemon(true);
        process.start();
        awaitResponse(); // Consume the startup banner and prompt before sending commands.
        return process;
    }

    private void readResponses() {
        StringBuilder pending = new StringBuilder();
        try (Reader reader = new InputStreamReader(checkerProcess.getInputStream(), StandardCharsets.UTF_8)) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                pending.append(buffer, 0, count);
                // Prompts have no newline and may span reads. Only inspect the trailing text.
                if (PROMPT.matcher(pending).region(Math.max(0, pending.length() - 32), pending.length()).find()) {
                    responses.offer(pending.toString());
                    pending.setLength(0);
                }
            }
            if (!stopped) responseFailure = new IOException("nuXmv exited before completing its response:\n" + pending);
        } catch (IOException failure) {
            if (!stopped) responseFailure = failure;
        } finally {
            responses.offer("");
            checkerProcess.destroyForcibly();
            try {
                checkerProcess.waitFor();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException shuttingDown) {
                    // JVM shutdown has already started the hook.
                }
            }
        }
    }

    public void stopNuXmvThread() throws IOException {
        stopped = true;
        responses.offer("");
        // Destroying the child releases both blocked reads and blocked command writes.
        if (checkerProcess != null) checkerProcess.destroyForcibly();
        boolean interrupted = Thread.interrupted();
        long deadline = java.lang.System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        try {
            while (process != null && process.isAlive()) {
                long remaining = deadline - java.lang.System.nanoTime();
                if (remaining <= 0) throw new IOException("nuXmv worker did not stop");
                try {
                    process.join(Math.max(1, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining)));
                } catch (InterruptedException cancellation) {
                    interrupted = true;
                }
            }
        } finally {
            try {
                if (commandInput != null) commandInput.close();
                Files.deleteIfExists(path);
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }

    public JSONObject outputToJSON(String output) {
        return outputToJSON(system, output);
    }

    @Override
    public void close() throws IOException {
        stopNuXmvThread();
    }
    private static final Pattern FRACTION = Pattern.compile("(-?)f'([0-9]+)/([0-9]+)");

    public static JSONObject outputToJSON(recipe.lang.System system, String nuxmvSimOutput){
        Set<String> channelFields = new HashSet<>();
        for (var instance : system.getAgentInstances()) {
            for (var variable : instance.getAgent().getStore().getAttributes().values()) {
                if (variable.getType().name().equals("channel"))
                    channelFields.add(instance.getLabel() + "-" + variable.getName());
            }
        }
        JSONObject jsonObject = new JSONObject();

        if(Pattern.compile("(?mi)^[ \t]*progress[ \t]*=[ \t]*FALSE[ \t]*$").matcher(nuxmvSimOutput).find()){
            // No interaction is enabled; an unchanged transition alone is not a deadlock.
            jsonObject.put("___STUCK___", true);
        }

        Set<String> stateVariables = new HashSet<>();
        system.getAgentInstances().forEach(ag -> {
            stateVariables.add(ag.getLabel() + "-automaton-state");
            ag.getAgent().getStore().getAttributes().keySet().forEach(name ->
                stateVariables.add(ag.getLabel() + "-" + name));
            ag.getAgent().getActions().forEach(action -> {
                if (action instanceof recipe.lang.process.BasicProcess) {
                    String label = ((recipe.lang.process.BasicProcess) action).getLabel();
                    if (label != null && !label.isEmpty()) stateVariables.add(ag.getLabel() + "-" + label);
                }
            });

        });

        nuxmvSimOutput = nuxmvSimOutput.replaceAll("(?m)^([ \t]*)(obs[0-9]+)([ \t]*=)", "$1___LTOL___-$2$3");
        nuxmvSimOutput = nuxmvSimOutput.replaceAll(
            "(?m)^([ \t]*)(no-observations)([ \t]*=)",
            "$1___LTOL___-$2$3");

        String agentStateRegex = "(^|\\n)[^=\\n\\^]+\\-[^=\\n\\^]+ =[^=\\n$]+(\\n|$)";
        Pattern compile = Pattern.compile(agentStateRegex, Pattern.MULTILINE);
        Matcher matcher = compile.matcher(nuxmvSimOutput);

        while(matcher.find()){
            String[] group = matcher.toMatchResult().group().split("=");

            String[] left = group[0].split("-", 2);

            String agent = left[0].trim();

            if(!stateVariables.contains(group[0].trim()) && !agent.equals("___LTOL___")){
                continue;
            }

            String var = left[1].trim();

            String val = group[1].trim().replaceAll(agent + "\\-", "");
            val = normalise(channelFields, group[0].trim(), val);


            if (!jsonObject.has(agent)){
                jsonObject.put(agent, new JSONObject());
            }

            ((JSONObject) jsonObject.get(agent)).put(var, val);
        }

        return jsonObject;
    }
    private static String normalise(Set<String> channelFields, String field, String value) {
        if (channelFields.contains(field) && value.equals(recipe.Config.nuxmvBroadcast)) return recipe.Config.broadcast;
        return normaliseNumber(value);
    }

    public static String interpreterNumber(String field, String value) {
        String decimal = normaliseNumber(value);
        try {
            new BigDecimal(decimal);
            return decimal;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Cannot load " + field + " = " + value
                    + ": the interpreter requires an exact finite decimal; this numeric value is unsupported.", invalid);
        }
    }

    private static String normaliseNumber(String value) {
        Matcher fraction = FRACTION.matcher(value);
        if (!fraction.matches()) return value;
        try {
            BigDecimal numerator = new BigDecimal(fraction.group(2));
            if (!fraction.group(1).isEmpty()) numerator = numerator.negate();
            return numerator.divide(new BigDecimal(fraction.group(3))).toPlainString();
        } catch (ArithmeticException nonDecimalValue) {
            // Do not silently round exact checker values that lack a finite decimal.
            return value;
        }
    }
}
