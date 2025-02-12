package recipe.analysis;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.json.JSONObject;

import recipe.Config;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.utils.Pair;

public class NuXmvInteraction {

    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    Thread process;
    boolean go = false;
    boolean go_msat = false;
    boolean simulationStarted = false;
    Path path;
    recipe.lang.System system;

    public NuXmvInteraction(recipe.lang.System system) throws Exception {
        this.system = system;
        String nuxmvScript = ToNuXmv.transform(system);
        if(Files.exists(Path.of("./forInteraction.smv"))) Files.delete(Path.of("./forInteraction.smv"));
        path = Files.createFile(Path.of("./forInteraction.smv"));
        Files.write(path, nuxmvScript.getBytes(StandardCharsets.UTF_8));
        process = startNuXmvThread();
    }

    public static boolean isAlive(Process p) {
        try {
            p.exitValue();
            return false;
        }
        catch (IllegalThreadStateException e) {
            return true;
        }
    }

    public String execute(Predicate<String> isFinished, String command) throws IOException{
        String outText = "";
        byteArrayOutputStream.reset();
        byteArrayOutputStream.flush();
        byteArrayOutputStream.flush();
        out.write((command + "\n").getBytes());
        out.flush();

        while (!isFinished.test(outText)) {
            nuxmvTurn.set(true);
            while (nuxmvTurn.get()) {
            }
            byteArrayOutputStream.flush();
            byteArrayOutputStream.flush();
            outText += new String(byteArrayOutputStream.toByteArray());
            outText = outText.replaceAll("(^|\\r|\\n)+\\*\\*\\*[^\\n|$]*($|\\r|\\n)+", "");
            byteArrayOutputStream.flush();
            byteArrayOutputStream.reset();
            out.write(("\n").getBytes());
            out.flush();
            if(outText.contains("not built")){
                return outText;
            }
        }
        return outText;
    }

    public Pair<Boolean, String> modelCheck(String property, Boolean bmc, int steps) throws IOException {
        if(bmc && !go_msat){
            Pair<Boolean, String> initialise = initialise(bmc);
            if(!initialise.getLeft()){
                return initialise;
            }
        } else if(!bmc && !go){
            Pair<Boolean, String> initialise = initialise(bmc);
            if(!initialise.getLeft()){
                return initialise;
            }
        }

        Predicate<String> finished = (x) ->{
            if(bmc){
                if(x.contains("no counterexample found with bound " + steps)
                        || x.replaceAll(" |\n|\r|\t|\\(|\\)", "").contains(property.trim().replaceAll(" |\n|\r|\t|\\(|\\)", "") + "is")) {
                    return true;
                } else{
                    return false;
                }
            } else{
                if(x.replaceAll(" |\n|\r|\t|\\(|\\)", "").contains(property.trim().replaceAll(" |\n|\r|\t|\\(|\\)", "") + "is")) {
                    return true;
                } else{
                    return false;
                }
            }
        };

        String out = "";
        out = execute(finished, ((system.isSymbolic() || bmc ? "msat_" : "") + "check_ltlspec" + (system.isSymbolic() || bmc ? "_bmc" : "") + " -p \"" + property + "\" "+ (system.isSymbolic() || bmc ? "-k " + steps : "")));
        out = out.replaceAll("nuXmv > ", "").trim();
        out = out.replaceAll("\n *(falsify-not-|keep-all|transition |progress )[^\\n$)]*(?=$|\\r?\\n)", "");
        return new Pair<>(true, out);
    }

    public Pair<Boolean, String> modelCheckic3(String property, boolean bounded, int steps) throws IOException {
        Pair<Boolean, String> initialise = initialise(true);
        if(!initialise.getLeft()){
            return initialise;
        }

        Predicate<String> finished = (x) -> {
            String s = x.replaceAll(" |\n|\r|\t|\\(|\\)", "");
            if(s.contains(property.trim().replaceAll(" |\n|\r|\t|\\(|\\)", "") + "is")) {
                return true;
            } else if(s.contains("syntaxerror")){
                return true;
            } else{
                return false;
            }
        };

        String out = "";
        out = execute(finished, ("check_ltlspec_ic3" + (bounded ? " -k " + steps : "") + " -p \"" + property + "\" "));
        out = out.replaceAll("nuXmv > ", "").trim();
        out = out.replaceAll("\n *(falsify-not-|keep-all|transition |progress )[^\\n$)]*(?=$|\\r?\\n)", "");
        if(out.contains("syntax error")){
            return new Pair<>(false, out);
        } else{
            return new Pair<>(true, out);
        }
    }

    public Pair<Boolean, String> initialise(boolean simulateOrBMC) throws IOException {
        String out = execute((x) -> x.trim().endsWith("nuXmv >"),"go" + ((system.isSymbolic() || simulateOrBMC) ? "_msat" : ""));
        if(system.isSymbolic() || simulateOrBMC){
            out += execute((x) -> x.trim().endsWith("nuXmv >"),"build_boolean_model");
        }

        if(out.contains(" file ")) {
            return new Pair<>(false, out);
        }

        return new Pair<>(true, out);
    }

    public Pair<Boolean, String> simulation_pick_init_state(String constraint) throws IOException {
        if(!go_msat){
            Pair<Boolean, String> initialise = initialise(true);
            initialise = initialise(true);
            if(!initialise.getLeft()){
                return initialise;
            }
        }
        String out = execute((x) -> !x.trim().toLowerCase(Locale.ROOT).replaceAll("( |nuxmv *>)", "").equals("") && x.trim().endsWith("nuXmv >"), "msat_pick_state -v -c \"" + constraint + "\"");
        if(out.contains(" file ") || out.contains("No trace")){
            return new Pair<>(false, out);
        }

        simulationStarted = true;
        return new Pair<>(true, parseLastState(out));
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
            return new Pair<>(true, parseLastState(out)); //To parse next state
        }
    }

    public Thread startNuXmvThread() throws IOException {
        Thread t = new Thread(() -> {
            try {
                runNuXmv(path, in, byteArrayOutputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();

        //waiting for initialisation
        while(nuxmvTurn.get()){}
        return t;
    }

    public void stopNuXmvThread() throws IOException {
        this.process.interrupt();
    }

    static AtomicReference<Boolean> nuxmvTurn = new AtomicReference<>(true);

    public static void runNuXmv(Path path, InputStream inr, OutputStream outw) throws Exception {
        String nuxmvPath = Config.getNuxmvPath();
        if (nuxmvPath.equals("nuxmv")) {
            boolean existsInPath = Stream.of(java.lang.System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
            .map(Paths::get)
            .anyMatch(p -> Files.exists(p.resolve("nuxmv")));
            if (!existsInPath) {
                throw new Exception("Looked for nuxmv on PATH, in \"./nuxmv/bin/nuxmv\", and in \"./bin/nuxmv\" but could find any nuXmv executable.");          
            }
        }

        ProcessBuilder builder = new ProcessBuilder(nuxmvPath, "-int", path.toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        Runnable runnable = () -> process.destroy();
        Runtime.getRuntime().addShutdownHook(new Thread(runnable));
        BufferedInputStream out = (BufferedInputStream) process.getInputStream();
        OutputStream in = process.getOutputStream();

        byte[] buffer = new byte[4000];
        List<String> textt = new ArrayList<>();

        while (isAlive(process)) {
            String text = new String(buffer, 0, buffer.length);
            int no = out.available();
            if (no > 0) {
                // int n = 
                out.read(buffer, 0, Math.min(no, buffer.length));
                text = new String(buffer, 0, Math.min(no, buffer.length));
                Thread.sleep(10);

                if(text.trim().replaceAll(" ", "").toLowerCase(Locale.ROOT).endsWith("nuxmv>")) {
                    textt.add(text);
                    outw.write(String.join("", textt).getBytes(StandardCharsets.UTF_8));
                    outw.flush();
                    outw.flush();
                    textt.clear();
                    buffer = new byte[4000];
                    nuxmvTurn.set(false);
                    while (!nuxmvTurn.get()) {
                    }
                } else{
                    textt.add(text);
                }
            }

            int ni = inr.available();
            if (ni > 0) {
                int n = inr.read(buffer, 0, Math.min(ni, buffer.length));
                text = new String(buffer, 0, n);
                in.write(buffer, 0, n);
                in.flush();
                buffer = new byte[4000];
            }

            try {
                //TODO need to parse nuxmv output to see if it is ready, depends on command
                Thread.sleep(10);
            } catch (InterruptedException e) {}
        }
        System.out.println(process.exitValue());
    }

    public JSONObject outputToJSON(String nuxmvSimOutput){
        JSONObject jsonObject = new JSONObject();

        if(nuxmvSimOutput.toLowerCase(Locale.ROOT).contains("keep-all = true")){
            // We're stuttering
            jsonObject.put("___STUCK___", true);
        }

        Set<String> receiveLabels = new HashSet<>();
        system.getAgentInstances().forEach(ag -> {
            for (ProcessTransition receiveTransition : ag.getAgent().getReceiveTransitions()) {
                String label = receiveTransition.getLabel().getLabel();
                if(label != null && !label.equals(""))
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

        match:
        while(matcher.find()){
            String[] group = matcher.toMatchResult().group().split("=");

            String[] left = group[0].split("-", 2);

            String agent = left[0].trim();

            if(agent.startsWith("falsify") || agent.startsWith("keep")){
                continue;
            }

            String var = left[1].trim();

            String val = group[1].trim().replaceAll(agent + "\\-", "");

            if(receiveLabels.contains(group[0].trim()) && val.equals("FALSE")){
                continue match;
            }

            if (!jsonObject.has(agent)){
                jsonObject.put(agent, new JSONObject());
            }

            ((JSONObject) jsonObject.get(agent)).put(var, val);
        }

        return jsonObject;
    }
}