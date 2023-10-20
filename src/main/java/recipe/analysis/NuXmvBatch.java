package recipe.analysis;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import recipe.Config;
import recipe.lang.System;
import recipe.lang.utils.Pair;

public class NuXmvBatch {
    Path path;

    public NuXmvBatch(System system) throws Exception {
        String nuxmvScript = ToNuXmv.transform(system);
        if(Files.exists(Path.of("./forInteraction.smv"))) Files.delete(Path.of("./forInteraction.smv"));
        path = Files.createFile(Path.of("./forInteraction.smv")).toRealPath();
        Files.write(path, nuxmvScript.getBytes(StandardCharsets.UTF_8));
    }

    private Path makeScript(boolean symbolic, boolean bounded, boolean build_boolean_model, int steps) throws IOException {
        Path scriptFile = Files.createTempFile("rcheck", "_script.smv");
        StringBuilder script = new StringBuilder    ("go_msat\n");
        if (build_boolean_model) script.append      ("build_boolean_model\n");
        script.append                               ("check_ltlspec_ic3");
        if (bounded) script.append                  (" -k " + steps);
        script.append                               ("\nquit\n");
        Files.write(scriptFile, script.toString().getBytes(StandardCharsets.UTF_8));

        return scriptFile;
    }

    private BufferedReader callNuXmv (Path scriptFile) throws IOException {
        String nuxmvPath = Config.getNuxmvPath();
        ProcessBuilder builder = new ProcessBuilder(nuxmvPath,  "-source", scriptFile.toRealPath().toString(), path.toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedInputStream out = (BufferedInputStream) process.getInputStream();
        return new BufferedReader(new InputStreamReader(out));
    }

    public Pair<Boolean, String> modelCheckic3(String property, boolean bounded, int steps) throws Exception {
        Path scriptFile = makeScript(true, bounded, true, steps);
        BufferedReader reader = callNuXmv(scriptFile);
        StringBuilder nuXmvOutput = new StringBuilder();
        String nextLine;
        Boolean gotError = false;
        Boolean infinitePrecision = false;
        while ((nextLine = reader.readLine()) != null) {
            if (nextLine.startsWith("*** ")) continue;
            nextLine = nextLine.replaceAll("\n *(falsify-not-|keep-all|transition |progress )[^\\n$)]*(?=$|\\r?\\n)", "");

            if (!nextLine.isBlank()){
                nuXmvOutput.append(nextLine);
                nuXmvOutput.append("\n");
                gotError |= nextLine.contains("syntax error");
                gotError |= nextLine.contains("aborting 'source");
                infinitePrecision |= nextLine.contains("Impossible to build a boolean FSM with infinite precision variables");
                if (gotError || infinitePrecision) break;
            }
        }
        Files.deleteIfExists(scriptFile);
        reader.close(); // Just to be sure
        if (!infinitePrecision) return new Pair<>(!gotError, nuXmvOutput.toString());
        // Previous call failed due to infinite-precision variables
        else {
            scriptFile = makeScript(true, bounded, false, steps);
            reader = callNuXmv(scriptFile);
            nuXmvOutput.setLength(0);
            while ((nextLine = reader.readLine()) != null) {
                if (nextLine.startsWith("*** ")) continue;
                // nextLine = nextLine.replaceAll("nuXmv > ", "").trim();
                nextLine = nextLine.replaceAll("\n *(falsify-not-|keep-all|transition |progress )[^\\n$)]*(?=$|\\r?\\n)", "");

                if (!nextLine.isBlank()){
                    nuXmvOutput.append(nextLine);
                    nuXmvOutput.append("\n");
                    gotError |= nextLine.contains("syntax error");
                    gotError |= nextLine.contains("aborting 'source");
                }
            }
            Files.deleteIfExists(scriptFile);
            reader.close(); // Just to be sure
            return new Pair<>(!gotError, nuXmvOutput.toString());
        }




        
    }
}
