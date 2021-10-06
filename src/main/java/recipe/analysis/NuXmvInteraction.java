package recipe.analysis;

import recipe.Config;
import recipe.lang.utils.Pair;

import org.json.*;

import javax.annotation.RegEx;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//        execute("\n");
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

    public String execute(String command) throws IOException{
        byteArrayOutputStream.reset();
        byteArrayOutputStream.flush();
        byteArrayOutputStream.flush();
        out.write((command + "\n").getBytes());
        out.flush();
        nuxmvTurn.set(true);
        while(nuxmvTurn.get()){}
        byteArrayOutputStream.flush();
        byteArrayOutputStream.flush();
        String out = new String(byteArrayOutputStream.toByteArray());
        byteArrayOutputStream.flush();
        byteArrayOutputStream.reset();
        return out;
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

        String out = "";
        while(out.toLowerCase(Locale.ROOT).replaceAll("( *\\*\\*\\*[^\n]*\n)|(nuxmv *>)", "").trim().equals(""))
            out = execute(((system.isSymbolic() || bmc ? "msat_" : "") + "check_ltlspec" + (system.isSymbolic() || bmc ? "_bmc" : "") + " -p \"" + property + "\" "+ (system.isSymbolic() || bmc ? "-k " + steps : "")));
        out = out.replaceAll("nuXmv > ", "").trim();
        out = out.replaceAll("\n *(falsify-not-|keep-all|transition |progress )[^\\n$)]*(?=$|\\r?\\n)", "");
        return new Pair<>(true, out);
    }

    public Pair<Boolean, String> initialise(boolean simulateOrBMC) throws IOException {
        String out = execute("go" + ((system.isSymbolic() | simulateOrBMC) ? "_msat" : ""));
//        out = execute("go" + ((system.isSymbolic() | simulateOrBMC) ? "_msat" : ""));
//        while(out.startsWith("*** This is nuXmv")) out = execute("go" + ((system.isSymbolic() | simulateOrBMC) ? "_msat" : ""));

        if(!out.contains("file ")) {
            if(simulateOrBMC)
                go_msat = true;
            else go = true;
            return new Pair<>(true, out);
        }

        return new Pair<>(false, out);
    }

    public Pair<Boolean, String> simulation_pick_init_state(String constraint) throws IOException {
        if(!go_msat){
            Pair<Boolean, String> initialise = initialise(true);
            initialise = initialise(true);
            if(!initialise.getLeft()){
                return initialise;
            }
        }
        String out = "";
        while(out.toLowerCase(Locale.ROOT).replaceAll("( *\\*\\*\\*[^\n]*\n)|(nuxmv *>)", "").trim().equals(""))
            out = execute("msat_pick_state -v -c \"" + constraint + "\"");
        execute("\n");
        if(out.contains("No trace")){
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

        String out = "";
        while(out.toLowerCase(Locale.ROOT).replaceAll("( *\\*\\*\\*[^\n]*\n)|(nuxmv *>)", "").trim().equals(""))
            out = execute("msat_simulate -k 1 -v -t \"" + constraint + "\"").replaceAll("(nuXmv >)", "").trim();
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
        String out = new String(byteArrayOutputStream.toByteArray());
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
            String ttt = Files.readString(path);
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
                    int n = out.read(buffer, 0, Math.min(no, buffer.length));
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

    public static JSONObject outputToJSON(String nuxmvSimOutput){
        JSONObject jsonObject = new JSONObject();

        if(nuxmvSimOutput.toLowerCase(Locale.ROOT).contains("keep-all = true")){
            jsonObject.put("stuck", true);
            return jsonObject;
        }

        String agentStateRegex = "(^|\\n)[^=\\n\\^]+\\-[^=\\n\\^]+ =[^=\\n$]+(\\n|$)";
        Pattern compile = Pattern.compile(agentStateRegex, Pattern.MULTILINE);
        Matcher matcher = compile.matcher(nuxmvSimOutput);

        while(matcher.find()){
            String[] group = matcher.toMatchResult().group().split("=");

            String[] left = group[0].split("-");

            String agent = left[0].trim();
            String var = left[1].trim();

            String val = group[1].trim().replaceAll(agent + "\\-", "");

            if (!jsonObject.has(agent)){
                jsonObject.put(agent, new JSONObject());
            }

            ((JSONObject) jsonObject.get(agent)).put(var, val);

//            String agentVarRegex = "(^|\\n) *(" + agent + ")\\-[^\\n=]+=[^=\\n$]+(\\n|$)";
//            Pattern compile2 = Pattern.compile(agentVarRegex, Pattern.MULTILINE);
//            Matcher matcher2 = compile2.matcher(nuxmvSimOutput);
//            JSONObject vars = new JSONObject();
//
//            while (matcher2.find()){
//                String[] group2 = matcher2.toMatchResult().group().split("=");
//
//                String[] left2 = group2[0].split("\\-");
//
//                String var2 = left2[1].trim();
//                if(var2.equals("state")){
//                    continue;
//                }
//
//                String val2 = group2[1].trim().replaceAll(agent + "\\-", "");
//
//                vars.put(var2, val2);
//            }
//
//            ((JSONObject) jsonObject.get(agent)).put("local", vars);
        }

        return jsonObject;
    }
}