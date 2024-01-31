package recipe.analysis;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import recipe.lang.agents.ProcessTransition;
import recipe.lang.ltol.LTOL;
import recipe.lang.utils.Pair;

import java.util.logging.Logger;


public class NuXmvBatch {
    System system;
    List<LTOL> allSpecs;

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
        this.allSpecs = new ArrayList<>(system.getSpecs());
    }

    public JSONObject outputToJSON(String nuxmvSimOutput) {
        JSONObject jsonObject = new JSONObject();

        if (nuxmvSimOutput.toLowerCase(Locale.ROOT).contains("keep-all = true")) {
            // We're stuttering
            jsonObject.put("___STUCK___", true);
        }

        Set<String> receiveLabels = new HashSet<>();
        system.getAgentInstances().forEach(ag -> {
            for (ProcessTransition receiveTransition : ag.getAgent().getReceiveTransitions()) {
                String label = receiveTransition.getLabel().getLabel();
                if (label != null && !label.equals(""))
                    receiveLabels.add(ag.getLabel() + "-" + receiveTransition.getLabel().getLabel());
            }
        });

        nuxmvSimOutput = nuxmvSimOutput.replaceAll("(obs[0-9]+)", "___LTOL___-$1");
        nuxmvSimOutput = nuxmvSimOutput.replaceAll(
                "\\bno-observations\\b",
                "___LTOL___-no-observations");

        String agentStateRegex = "(^|\\n)[^=\\n\\^]+\\-[^=\\n\\^]+ =[^=\\n$]+(\\n|$)";
        Pattern compile = Pattern.compile(agentStateRegex, Pattern.MULTILINE);
        Matcher matcher = compile.matcher(nuxmvSimOutput);

        match: while (matcher.find()) {
            String[] group = matcher.toMatchResult().group().split("=");

            String[] left = group[0].split("-", 2);

            String agent = left[0].trim();

            if (agent.startsWith("falsify") || agent.startsWith("keep")) {
                continue;
            }

            String var = left[1].trim();

            String val = group[1].trim().replaceAll(agent + "\\-", "");

            if (receiveLabels.contains(group[0].trim()) && val.equals("FALSE")) {
                continue match;
            }

            if (!jsonObject.has(agent)) {
                jsonObject.put(agent, new JSONObject());
            }

            ((JSONObject) jsonObject.get(agent)).put(var, val);
        }

        return jsonObject;
    }

    public Pair<Boolean, String> pickInitialState(String constraint) throws Exception {
        Path scriptFile = Files.createTempFile("rcheck", "_script.smv");
        Path modelFile = Files.createTempFile("rcheck", "_model.smv");
        Files.write(scriptFile, ("go_msat\nmsat_pick_state -v -c \"" + constraint + "\"\nquit").getBytes(StandardCharsets.UTF_8));
        String nuxmvScript;
        synchronized (system) {
            system.setSpecs(new ArrayList<LTOL>());
            nuxmvScript = ToNuXmv.transform(system);
        }
        Files.write(modelFile, nuxmvScript.getBytes(StandardCharsets.UTF_8));
        NuXmvResult result = callAndRead(scriptFile, modelFile);
        Files.deleteIfExists(modelFile);
        return result.toPair();
    }

    private NuXmvResult callAndRead(Path scriptFile, Path modelFile) throws IOException {
        Process proc = callNuXmv(scriptFile, modelFile);
        processes.add(proc);
        NuXmvResult result = readFrom(proc);
        processes.remove(proc);
        proc.destroy();
        Files.deleteIfExists(scriptFile);
        return result;
    }

    enum Cmd {
        GO_MSAT,
        BUILD_BOOLEAN,
        BMC,
        INC_BMC,
        IC3_LTL,
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
                    script.append("msat_check_ltlspec_sbmc_inc");
                    script.append(String.format(" -p \"%s\"", property));
                    break;
                case IC3_INVAR:
                    script.append("check_property_as_invar_ic3");
                    script.append(String.format(" -L \"%s\"", property));
                    if (bound >= 0) { script.append(" -k " + bound); }
                    break;
                case IC3_LTL:
                    script.append("check_ltlspec_ic3");
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

    private Process callNuXmv(Path scriptFile, Path modelFile) throws IOException {
        String nuxmvPath = Config.getNuxmvPath();
        ProcessBuilder builder = new ProcessBuilder(nuxmvPath, "-source", scriptFile.toRealPath().toString(), modelFile.toRealPath().toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        processes.add(process);
        return process;
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
            nextLine = nextLine.replaceAll("\n *(falsify-not-|keep-all|transition |progress )[^\\n$)]*(?=$|\\r?\\n)",
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
        result.output = outputBuilder.toString();
        reader.close(); // Just to be sure
        return result;
    }

    public Pair<Boolean, String> modelCheckIc3(String property, boolean bounded, int steps, int propertyIndex) throws Exception {
        if (!bounded) steps = -1;
        Path scriptFile = newNuxmvScript(steps, property, Cmd.GO_MSAT, Cmd.BUILD_BOOLEAN, Cmd.IC3_INVAR, Cmd.QUIT);
        Path modelFile = Files.createTempFile("rcheck", "_model.smv");
        String nuxmvScript;
        synchronized (system) {
            List<LTOL> singleton = new ArrayList<>();
            singleton.add(allSpecs.get(propertyIndex));
            system.setSpecs(singleton);
            nuxmvScript = ToNuXmv.transform(system);
        }
        Files.write(modelFile, nuxmvScript.getBytes(StandardCharsets.UTF_8));

        NuXmvResult result = callAndRead(scriptFile, modelFile);
        if (result.success()) return result.toPair();
        // Previous call failed due to infinite-precision variables
        else if (result.err == Error.NOT_INVAR) {
            // Property was not an INVARSPEC
            scriptFile = newNuxmvScript(steps, property, Cmd.GO_MSAT, Cmd.BUILD_BOOLEAN, Cmd.IC3_LTL, Cmd.QUIT);
            result = callAndRead(scriptFile, modelFile);
            return result.toPair();
        }
        else if (result.err == Error.INF_PRECISION) {
            // try INVARSPEC checking without boolean model
            scriptFile = newNuxmvScript(steps, property, Cmd.GO_MSAT, Cmd.IC3_INVAR, Cmd.QUIT);
            result = callAndRead(scriptFile, modelFile);
            if (result.success()) return result.toPair();
            scriptFile = newNuxmvScript(steps, property, Cmd.GO_MSAT, Cmd.IC3_LTL, Cmd.QUIT);
            return callAndRead(scriptFile, modelFile).toPair();
        }
        // If we end up here, there's something wrong
        result.err = Error.OTHER;
        return result.toPair();
    }

    public Pair<Boolean, String> modelCheckBmc(String property, boolean bounded, int steps, int propertyIndex) throws Exception {
        if (!bounded) steps = -1;
        Cmd bmcCommand = bounded ? Cmd.BMC : Cmd.INC_BMC;
        Path scriptFile = newNuxmvScript(steps, property, Cmd.GO_MSAT, Cmd.BUILD_BOOLEAN, bmcCommand, Cmd.QUIT);
        Path modelFile = Files.createTempFile("rcheck", "_model.smv");
        String nuxmvScript;
        synchronized (system) {
            List<LTOL> singleton = new ArrayList<>();
            singleton.add(allSpecs.get(propertyIndex));
            system.setSpecs(singleton);
            nuxmvScript = ToNuXmv.transform(system);
        }
        NuXmvResult result = callAndRead(scriptFile, modelFile);
        if (result.success()) return result.toPair();
        
        else if (result.err == Error.INF_PRECISION) {
            scriptFile = newNuxmvScript(steps, property, Cmd.GO_MSAT, bmcCommand, Cmd.QUIT);
            return callAndRead(scriptFile, modelFile).toPair();
        }
        // If we end up here, there's something wrong
        result.err = Error.OTHER;
        return result.toPair();
    }
}
