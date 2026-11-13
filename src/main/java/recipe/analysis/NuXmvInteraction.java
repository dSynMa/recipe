package recipe.analysis;

import java.math.BigDecimal;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import recipe.Config;
import recipe.lang.utils.Pair;

public class NuXmvInteraction {

    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    Thread process;
    boolean go = false;
    boolean go_msat = false;
    private boolean booleanModelBuilt;
    private boolean booleanModelUnavailable;
    boolean simulationStarted = false;
    Path path;
    recipe.lang.System system;

    public NuXmvInteraction(recipe.lang.System system) throws Exception {
        this.system = system;
        String nuxmvScript = ToNuXmv.transform(system);
        path = Files.createTempFile("rcheck-interactive-", ".smv");
        path.toFile().deleteOnExit();
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

    private static boolean booleanEncodingFailed(String output) {
        return output.contains("Impossible to build a boolean FSM with infinite precision variables")
                || output.contains("not suitable constants for operator")
                // nuXmv 2.0 reports unsupported FLOOR nodes as a Boolean encoder error.
                || output.contains("bdd_enc_eval_recur:");
    }

    public Pair<Boolean, String> modelCheck(String property, Boolean bmc, int steps) throws IOException {
        if ((system.isSymbolic() || booleanModelUnavailable) && !bmc && steps < 0)
            return modelCheckic3(property, false, steps);
        if(bmc && !go_msat){
            Pair<Boolean, String> initialise = initialise(bmc || (system.isSymbolic() || booleanModelUnavailable));
            if(!initialise.getLeft()){
                return initialise;
            }
        } else if(!bmc && !go){
            Pair<Boolean, String> initialise = initialise(bmc || (system.isSymbolic() || booleanModelUnavailable));
            if(!initialise.getLeft()){
                return initialise;
            }
        }

        if (booleanModelUnavailable && !bmc && steps < 0)
            return modelCheckic3(property, false, steps);
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
        out = execute(finished, (((system.isSymbolic() || booleanModelUnavailable) || bmc ? "msat_" : "") + "check_ltlspec" + ((system.isSymbolic() || booleanModelUnavailable) || bmc ? "_bmc" : "") + " -p \"" + property + "\" "+ ((system.isSymbolic() || booleanModelUnavailable) || bmc ? "-k " + steps : "")));
        if (!bmc && booleanEncodingFailed(out)) {
            Pair<Boolean, String> smt = initialise(true);
            if (!smt.getLeft()) return smt;
            String command = steps < 0 ? "check_ltlspec_ic3 -i" : "msat_check_ltlspec_bmc -k " + steps;
            out = execute(finished, command + " -p \"" + property + "\"");
        }
        out = out.replaceAll("nuXmv > ", "").trim();
        out = out.replaceAll("\n *(falsify-not-|keep-all|transition )[^\\n$)]*(?=$|\\r?\\n)", "");
        return new Pair<>(true, out);
    }

    public Pair<Boolean, String> modelCheckic3(String property, boolean bounded, int steps) throws IOException {
        Pair<Boolean, String> initialise = initialise(true);
        if(!initialise.getLeft()){
            return initialise;
        }

        // nuXmv 2.0's finite IC3 engine does not enforce -k; use SMT for frame limits.
        boolean useBooleanModel = !bounded && !system.isSymbolic() && !booleanModelUnavailable;
        if (useBooleanModel && !booleanModelBuilt) {
            String output = execute(text -> text.trim().endsWith("nuXmv >"), "build_boolean_model");
            if (booleanEncodingFailed(output)) {
                booleanModelUnavailable = true;
                useBooleanModel = false;
            } else {
                if (Pattern.compile("(?m)^\\s*file .+: line [0-9]+:").matcher(output).find())
                    return new Pair<>(false, output);
                booleanModelBuilt = true;
            }
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
        out = execute(finished, ("check_ltlspec_ic3" + (useBooleanModel ? "" : " -i") + (bounded ? " -k " + steps : "") + " -p \"" + property + "\" "));
        if (useBooleanModel && booleanEncodingFailed(out))
            out = execute(finished, "check_ltlspec_ic3 -i -p \"" + property + "\"");
        out = out.replaceAll("nuXmv > ", "").trim();
        out = out.replaceAll("\n *(falsify-not-|keep-all|transition )[^\\n$)]*(?=$|\\r?\\n)", "");
        if(out.contains("syntax error")){
            return new Pair<>(false, out);
        } else{
            return new Pair<>(true, out);
        }
    }

    public Pair<Boolean, String> initialise(boolean simulateOrBMC) throws IOException {
        String out = execute((x) -> x.trim().endsWith("nuXmv >"),"go" + (((system.isSymbolic() || booleanModelUnavailable) || simulateOrBMC) ? "_msat" : ""));
        if (!simulateOrBMC && !system.isSymbolic() && !booleanModelUnavailable && booleanEncodingFailed(out)) {
            booleanModelUnavailable = true;
            return initialise(true);
        }
        // SMT simulation supports real arithmetic directly; Boolean conversion can
        // reject valid real expressions even when every state variable is finite.

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

        ProcessBuilder builder = null;
        try{
            builder = new ProcessBuilder(nuxmvPath, "-int", path.toString());
        } catch (Exception e){
            if(e.getMessage().contains("The system cannot find the file specified")){
                throw new Exception("Looked for nuxmv on PATH, in \"./nuxmv/bin/nuxmv\", and in \"./bin/nuxmv\" but could find any nuXmv executable.");
            }
            throw e;
        } finally {
            builder.redirectErrorStream(true);
            Process process = builder.start();
            Runnable runnable = () -> process.destroy();
            Runtime.getRuntime().addShutdownHook(new Thread(runnable));

            // String ttt = Files.readString(path);
            BufferedInputStream out = (BufferedInputStream) process.getInputStream();
            OutputStream in = process.getOutputStream();

            byte[] buffer = new byte[4000];
            List<String> textt = new ArrayList<>();

            while (isAlive(process)) {
                String text = new String(buffer, 0, buffer.length);
                int no = out.available();
//            buffer = out.readNBytes(40);
//            text = new String(out.readNBytes(160));

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
                } catch (InterruptedException e) {
                }

            }

            System.out.println(process.exitValue());
        }
    }

    public JSONObject outputToJSON(String nuxmvSimOutput){
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
            val = NuXmvInteraction.normalise(system, group[0].trim(), val);


            if (!jsonObject.has(agent)){
                jsonObject.put(agent, new JSONObject());
            }

            ((JSONObject) jsonObject.get(agent)).put(var, val);
        }

        return jsonObject;
    }
    private static final Pattern FRACTION = Pattern.compile("(-?)f'([0-9]+)/([0-9]+)");

    static String normalise(recipe.lang.System system, String field, String value) {
        if (value.equals(recipe.Config.nuxmvBroadcast)) {
            for (var instance : system.getAgentInstances()) {
                for (var variable : instance.getAgent().getStore().getAttributes().values()) {
                    if (variable.getType().name().equals("channel")
                            && field.equals(instance.getLabel() + "-" + variable.getName()))
                        return recipe.Config.broadcast;
                }
            }
        }
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
