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
import recipe.lang.agents.ProcessTransition;
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
        if (Files.exists(Path.of("./forInteraction.smv")))
            Files.delete(Path.of("./forInteraction.smv"));
        path = Files.createFile(Path.of("./forInteraction.smv")).toRealPath();
        Files.write(path, nuxmvScript.getBytes(StandardCharsets.UTF_8));
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

    public Pair<Boolean, String> pickInitialState(String constraint) throws IOException {
        Path scriptFile = Files.createTempFile("rcheck", "_script.smv");
        Files.write(scriptFile, ("go_msat\nmsat_pick_state -v -c \"" + constraint + "\"\nquit").getBytes(StandardCharsets.UTF_8));
        NuXmvResult result = callAndRead(scriptFile);
        Files.delete(scriptFile);
        return result.toPair();
    }

    private NuXmvResult callAndRead(Path scriptFile) throws IOException {
        Process proc = callNuXmv(scriptFile);
        processes.add(proc);
        NuXmvResult result = readFrom(proc);
        processes.remove(proc);
        proc.destroy();
        return result;
    }

    private Path makeScript(boolean build_boolean_model, int steps, boolean bmc) throws IOException {
        if (bmc && steps < 1)
            steps = 1;
        Path scriptFile = Files.createTempFile("rcheck", "_script.smv");
        StringBuilder script = new StringBuilder("go_msat\n");
        if (build_boolean_model)
            script.append("build_boolean_model\n");
        if (bmc)
            script.append("msat_check_ltlspec_bmc");
        else
            script.append("check_ltlspec_ic3");
        if (steps != -1)
            script.append(" -k " + steps);
        script.append("\nquit\n");
        Files.write(scriptFile, script.toString().getBytes(StandardCharsets.UTF_8));

        return scriptFile;
    }

    private Process callNuXmv(Path scriptFile) throws IOException {
        String nuxmvPath = Config.getNuxmvPath();
        ProcessBuilder builder = new ProcessBuilder(nuxmvPath, "-source", scriptFile.toRealPath().toString(),
                path.toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        processes.add(process);
        return process;
    }

    private class NuXmvResult {
        public boolean gotError;
        public boolean infinitePrecisionError;
        public String output;

        public Pair<Boolean, String> toPair() {
            return new Pair<>(!gotError, output.toString());
        }
    }

    private NuXmvResult readFrom(Process process) throws IOException {
        BufferedInputStream out = (BufferedInputStream) process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(out));
        StringBuilder outputBuilder = new StringBuilder();
        String nextLine;
        NuXmvResult result = new NuXmvResult();
        result.gotError = false;
        result.infinitePrecisionError = false;
        while ((nextLine = reader.readLine()) != null) {
            logger.finest(nextLine);
            if (nextLine.startsWith("*** internal error ***"))
                result.gotError = true;
            if (nextLine.startsWith("*** "))
                continue;
            nextLine = nextLine.replaceAll("\n *(falsify-not-|keep-all|transition |progress )[^\\n$)]*(?=$|\\r?\\n)",
                    "");

            if (!nextLine.isBlank()) {
                outputBuilder.append(nextLine);
                outputBuilder.append("\n");
                result.gotError |= nextLine.contains("syntax error");
                result.gotError |= nextLine.contains("Type System Violation");
                result.gotError |= nextLine.contains("Parsing error");
                result.gotError |= nextLine.contains("aborting 'source");
                result.infinitePrecisionError |= nextLine
                        .contains("Impossible to build a boolean FSM with infinite precision variables");
                if (result.gotError || result.infinitePrecisionError)
                    break;
            }
        }
        result.output = outputBuilder.toString();
        reader.close(); // Just to be sure
        return result;
    }

    public NuXmvResult exec(Path scriptFile) throws IOException {
        Process process = callNuXmv(scriptFile);
        NuXmvResult result = readFrom(process);
        process.destroy();
        processes.remove(process);
        Files.deleteIfExists(scriptFile);
        return result;
    }

    public Pair<Boolean, String> modelCheck(String property, boolean bounded, int steps, boolean bmc) throws Exception {
        if (!bmc && !bounded)
            steps = -1;
        Path scriptFile = makeScript(true, steps, bmc);
        NuXmvResult result = exec(scriptFile);
        if (!result.infinitePrecisionError) return result.toPair();
        // Previous call failed due to infinite-precision variables
        scriptFile = makeScript(false, steps, bmc);
        result = exec(scriptFile);
        return result.toPair();
    }
}
