package recipe.analysis;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import recipe.Config;
import recipe.lang.System;
import recipe.lang.utils.Pair;

import java.util.logging.Logger;


public class NuXmvBatch {
    Path path;
    System system;

    private static Logger logger = Logger.getLogger(NuXmvBatch.class.getName());
    public static LinkedList<Process> processes = new LinkedList<>();

    public static void stopAllProcesses() {
        logger.warning(" NuXmvBatch.stopAllProcesses() invoked");
        while (!processes.isEmpty()) {
            Process proc = processes.removeLast();
            proc.destroy();
            logger.info("Destroyed process " + proc.toString());
        }
    }

    public NuXmvBatch(System system) throws Exception {
        this.system = system;
        String nuxmvScript = ToNuXmv.transform(system);
        path = Files.createTempFile("rcheck-model-", ".smv");
        path.toFile().deleteOnExit();
        Files.write(path, nuxmvScript.getBytes(StandardCharsets.UTF_8));
    }

    public JSONObject outputToJSON(String nuxmvSimOutput) {
        JSONObject jsonObject = new JSONObject();

        if (Pattern.compile("(?mi)^[ \t]*progress[ \t]*=[ \t]*FALSE[ \t]*$").matcher(nuxmvSimOutput).find()) {
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

        while (matcher.find()) {
            String[] group = matcher.toMatchResult().group().split("=");

            String[] left = group[0].split("-", 2);

            String agent = left[0].trim();

            if (!stateVariables.contains(group[0].trim()) && !agent.equals("___LTOL___")) {
                continue;
            }

            String var = left[1].trim();

            String val = group[1].trim().replaceAll(agent + "\\-", "");
            val = NuXmvInteraction.normalise(system, group[0].trim(), val);


            if (!jsonObject.has(agent)) {
                jsonObject.put(agent, new JSONObject());
            }

            ((JSONObject) jsonObject.get(agent)).put(var, val);
        }

        return jsonObject;
    }

    public Pair<Boolean, String> pickInitialState(String constraint) throws IOException {
        Path scriptFile = Files.createTempFile("rcheck", "_script.smv");
        Files.write(scriptFile, ("go_msat\nmsat_pick_state -v -c \"" + constraint + "\"\nquit").getBytes(StandardCharsets.UTF_8));
        NuXmvResult result = callAndRead(scriptFile);
        return result.toPair();
    }

    private NuXmvResult callAndRead(Path scriptFile) throws IOException {
        Process proc = null;
        try {
            proc = callNuXmv(scriptFile);
            processes.add(proc);
            return readFrom(proc);
        } finally {
            if (proc != null) {
                processes.remove(proc);
                proc.destroy();
            }
            Files.deleteIfExists(scriptFile);
        }
    }

    enum Cmd {
        GO_MSAT,
        BUILD_BOOLEAN,
        BMC,
        INC_BMC,
        IC3_LTL,
        IC3_BOOLEAN,
        IC3_INVAR,
        QUIT
    }

    public static Path newNuxmvScript (int bound, String property, Cmd ... commands) throws IOException {
        Path path = Files.createTempFile("rcheck", "_script.smv");
        StringBuilder script = new StringBuilder();
        for (Cmd command : commands) {
            switch (command) {
                case GO_MSAT:
                    script.append("go_msat");
                    break;
                case BUILD_BOOLEAN:
                    script.append("build_boolean_model");
                    break;
                case BMC:
                    script.append("msat_check_ltlspec_bmc");
                    script.append(String.format(" -p \"%s\"", property));
                    script.append(" -k " + bound);
                    break;
                case INC_BMC:
                    // No -k silently stops at bmc_length (10 by default). Completeness
                    // checking permits a proof instead of only searching for counterexamples.
                    script.append("msat_check_ltlspec_sbmc_inc -c -k " + Integer.MAX_VALUE);
                    script.append(String.format(" -p \"%s\"", property));
                    break;
                case IC3_INVAR:
                    script.append("check_property_as_invar_ic3 -i");
                    script.append(String.format(" -L \"%s\"", property));
                    break;
                case IC3_BOOLEAN:
                    script.append("check_ltlspec_ic3");
                    script.append(String.format(" -p \"%s\"", property));
                    break;
                case IC3_LTL:
                    script.append("check_ltlspec_ic3 -i");
                    script.append(String.format(" -p \"%s\"", property));
                    if (bound >= 0) { script.append(" -k " + bound); }
                    break;
                case QUIT:
                    script.append("quit");
                    break;
                default:
                    throw new IOException("Unsupported command.");
            }
            script.append("\n");
        }
        Files.write(path, script.toString().getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private Process callNuXmv(Path scriptFile) throws IOException {
        String nuxmvPath = Config.getNuxmvPath();
        ProcessBuilder builder = new ProcessBuilder(nuxmvPath, "-source", scriptFile.toRealPath().toString(),
                path.toString());
        builder.redirectErrorStream(true);
        return builder.start();
    }

    enum Error {
        SYNTAX, TYPE, INF_PRECISION, NOT_INVAR, OTHER
    }

    private class NuXmvResult {
        // public boolean gotError;
        public Error err;
        // public boolean infinitePrecisionError;
        public String output;

        public boolean success() {
            return err == null;
        }

        public Pair<Boolean, String> toPair() {
            return new Pair<>(success(), output.toString());
        }
    }

    private NuXmvResult readFrom(Process process) throws IOException {
        BufferedInputStream out = (BufferedInputStream) process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(out));
        StringBuilder outputBuilder = new StringBuilder();
        String nextLine;
        NuXmvResult result = new NuXmvResult();
        while ((nextLine = reader.readLine()) != null) {
            logger.finest(nextLine);
            if (nextLine.startsWith("*** internal error ***")) {
                result.err = Error.OTHER;
            }
            if (nextLine.startsWith("*** "))
                continue;
            nextLine = nextLine.replaceAll("\n *(falsify-not-|keep-all|transition )[^\\n$)]*(?=$|\\r?\\n)",
                    "");

            if (!nextLine.isBlank()) {
                outputBuilder.append(nextLine);
                outputBuilder.append("\n");
                if (nextLine.contains("syntax error")) result.err = Error.SYNTAX;
                else if (nextLine.contains("Type System Violation")) result.err = Error.TYPE;
                else if (nextLine.contains("cannot be converted into INVARSPEC")) result.err = Error.NOT_INVAR;
                else if (nextLine.contains("Parsing error")) result.err = Error.OTHER;
                else if (nextLine.contains("aborting 'source")) result.err = Error.OTHER;
                else if (nextLine.contains("Impossible to build a boolean FSM with infinite precision variables")) result.err = Error.INF_PRECISION;
                if (!result.success())
                    break;
            }
        }
        reader.close();
        // At EOF, a crash or external termination must not become a successful
        // invocation merely because its output lacks a recognised diagnostic.
        if (result.success()) {
            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    result.err = Error.OTHER;
                    outputBuilder.append("nuXmv exited with status ").append(exitCode).append('\n');
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for nuXmv", e);
            }
        }
        result.output = outputBuilder.toString();
        return result;
    }

    private static boolean booleanEncodingFailed(String output) {
        return output.contains("Impossible to build a boolean FSM with infinite precision variables")
                || output.contains("not suitable constants for operator")
                // nuXmv 2.0 reports unsupported FLOOR nodes as a Boolean encoder error.
                || output.contains("bdd_enc_eval_recur:");
    }

    public Pair<Boolean, String> modelCheckIc3(String property, boolean bounded, int steps) throws Exception {
        if (!bounded) steps = -1;
        // Select the Boolean backend only for compatible, unbounded checks.
        boolean useBooleanModel = !bounded && !system.isSymbolic();
        Path scriptFile = useBooleanModel
                ? newNuxmvScript(steps, property, Cmd.GO_MSAT, Cmd.BUILD_BOOLEAN, Cmd.IC3_BOOLEAN, Cmd.QUIT)
                : newNuxmvScript(steps, property, Cmd.GO_MSAT, Cmd.IC3_LTL, Cmd.QUIT);
        NuXmvResult result = callAndRead(scriptFile);
        if (useBooleanModel && booleanEncodingFailed(result.output)) {
            scriptFile = newNuxmvScript(steps, property, Cmd.GO_MSAT, Cmd.IC3_LTL, Cmd.QUIT);
            result = callAndRead(scriptFile);
        }
        return result.toPair();
    }

    public Pair<Boolean, String> modelCheckBmc(String property, boolean bounded, int steps) throws Exception {
        if (!bounded) steps = -1;
        Cmd bmcCommand = bounded ? Cmd.BMC : Cmd.INC_BMC;
        // go_msat prepares the SMT model. Building a Boolean model additionally would
        // reject real expressions even when all declared state variables are finite.
        Path scriptFile = newNuxmvScript(steps, property, Cmd.GO_MSAT, bmcCommand, Cmd.QUIT);
        return callAndRead(scriptFile).toPair();
    }
}
