package recipe.analysis;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import recipe.Config;
import recipe.lang.System;
import recipe.lang.utils.Pair;

public class NuXmvBatch {
    Path path;
    public static LinkedList<Process> processes = new LinkedList<>();
    

    public static void stopAllProcesses() {
        while (!processes.isEmpty()) {
            Process proc = processes.removeLast();
            java.lang.System.out.printf("Destroying %s...", proc.toString());
            proc.destroy();
            java.lang.System.out.println(" done.");
        }
    }

    public NuXmvBatch(System system) throws Exception {
        String nuxmvScript = ToNuXmv.transform(system);
        if(Files.exists(Path.of("./forInteraction.smv"))) Files.delete(Path.of("./forInteraction.smv"));
        path = Files.createFile(Path.of("./forInteraction.smv")).toRealPath();
        Files.write(path, nuxmvScript.getBytes(StandardCharsets.UTF_8));
    }

    private Path makeScript(boolean build_boolean_model, int steps, boolean bmc) throws IOException {
        if (bmc && steps < 1) steps = 1;
        Path scriptFile = Files.createTempFile("rcheck", "_script.smv");
        StringBuilder script = new StringBuilder    ("go_msat\n");
        if (build_boolean_model) script.append      ("build_boolean_model\n");
        if (bmc) script.append                      ("msat_check_ltlspec_bmc");
        else     script.append                      ("check_ltlspec_ic3");
        if (steps != -1) script.append              (" -k " + steps);
        script.append                               ("\nquit\n");
        Files.write(scriptFile, script.toString().getBytes(StandardCharsets.UTF_8));

        return scriptFile;
    }

    private Process callNuXmv (Path scriptFile) throws IOException {
        String nuxmvPath = Config.getNuxmvPath();
        ProcessBuilder builder = new ProcessBuilder(nuxmvPath,  "-source", scriptFile.toRealPath().toString(), path.toString());
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
            java.lang.System.out.println(nextLine);
            if (nextLine.startsWith("*** ")) continue;
            nextLine = nextLine.replaceAll("\n *(falsify-not-|keep-all|transition |progress )[^\\n$)]*(?=$|\\r?\\n)", "");

            if (!nextLine.isBlank()){
                outputBuilder.append(nextLine);
                outputBuilder.append("\n");
                result.gotError |= nextLine.contains("syntax error");
                result.gotError |= nextLine.contains("aborting 'source");
                result.infinitePrecisionError |= nextLine.contains("Impossible to build a boolean FSM with infinite precision variables");
                if (result.gotError || result.infinitePrecisionError) break;
            }
        }
        result.output = outputBuilder.toString();
        reader.close(); // Just to be sure
        return result;
    }

    public Pair<Boolean, String> modelCheck(String property, boolean bounded, int steps, boolean bmc) throws Exception {
        if (!bmc && !bounded) steps = -1;
        Path scriptFile = makeScript(true, steps, bmc);
        // BufferedReader reader = callNuXmv(scriptFile);
        Process process = callNuXmv(scriptFile);
        NuXmvResult result = readFrom(process);
        process.destroy();
        processes.remove(process);
        Files.deleteIfExists(scriptFile);
        if (!result.infinitePrecisionError) return result.toPair();
        // Previous call failed due to infinite-precision variables
        else {
            scriptFile = makeScript(false, steps, bmc);
            process = callNuXmv(scriptFile);
            result = readFrom(process);
            process.destroy();
            processes.remove(process);
            Files.deleteIfExists(scriptFile);
            return result.toPair();
        }




        
    }
}
