package recipe.analysis;

import recipe.lang.utils.Pair;

import org.json.*;

import javax.annotation.RegEx;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NuXmvInteraction {

    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    Thread process;
    boolean started = false;
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

    public String execute(String command) throws IOException{
        byteArrayOutputStream.reset();
        out.write((command + "\n").getBytes());
        out.flush();
        nuxmvTurn.set(true);
        while(nuxmvTurn.get()){}
        String out = new String(byteArrayOutputStream.toByteArray());
        return out;
    }

    public Pair<Boolean, String> modelCheck(String property, int steps) throws IOException {
        if(!started){
            Pair<Boolean, String> initialise = initialise();
            if(!initialise.getLeft()){
                return initialise;
            }
        }
        String out = execute(((system.isSymbolic() ? "msat_" : "") + "check_ltlspec" + (system.isSymbolic() ? "_bmc" : "") + " -p \"" + property + "\" "+ (system.isSymbolic() ? "-k " + steps : "")));
        out = out.replaceAll("nuXmv > ", "").trim();
        out = out.replaceAll("\n *(falsify-not-|keep-all|transition )[^\\n$)]*(?=$|\\r?\\n)", "");
        return new Pair<>(true, out);
    }

    public Pair<Boolean, String> initialise() throws IOException {
        String out = execute("go" + (system.isSymbolic() ? "_msat" : ""));
        out = execute("go" + (system.isSymbolic() ? "_msat" : ""));
        while(out.startsWith("*** This is nuXmv")) out = execute("go_msat");

        if(!out.contains("file ")) {
            started = true;
            return new Pair<>(true, out);
        }

        return new Pair<>(false, out);
    }

    public Pair<Boolean, String> simulation_pick_init_state(String constraint) throws IOException {
        if(!started){
            Pair<Boolean, String> initialise = initialise();
            if(!initialise.getLeft()){
                return initialise;
            }
        }
        String out = execute((system.isSymbolic() ? "msat_" : "") + "pick_state -v -c \"" + constraint + "\"");
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
        if(!started){
            Pair<Boolean, String> initialise = initialise();
            if(!initialise.getLeft()){
                return initialise;
            }
        }
        if(!simulationStarted) return simulation_pick_init_state(constraint);

        String out = execute((system.isSymbolic() ? "msat_" : "") + "simulate -k 1 -v -t \"" + constraint + "\"").replaceAll("(nuXmv >)", "").trim();
        if(system.isSymbolic()) {
            if (out.contains("UNSAT")) {
                return new Pair<>(false, out.replaceAll("\r\n|\n|nuXmv >", " ").split("\\*\\*\\*\\*")[0].trim());
            } else {
                return new Pair<>(true, parseLastState(out)); //To parse next state
            }
        } else{
            if (out.contains("No future state exists")){
                return new Pair<>(false, out.replaceAll("\r\n|\n|nuXmv >", " ").split("\\*\\*\\*\\*")[0].trim());
            } else {
                return new Pair<>(true, parseLastState(out)); //To parse next state
            }
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
        String nuxmvPath = "";
        if(Files.exists(Path.of("./nuxmv/bin/nuxmv")) || Files.exists(Path.of("./nuxmv/bin/nuxmv.exe"))){
            nuxmvPath = "./nuxmv/bin/nuxmv";
        } else if(Files.exists(Path.of("./bin/nuxmv")) || Files.exists(Path.of("./bin/nuxmv.exe"))){
            nuxmvPath = "./bin/nuxmv";
        } else {
            nuxmvPath = "nuxmv";
        }

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
            while (isAlive(process)) {
                String text = new String(buffer, 0, buffer.length);
                int no = out.available();
//            buffer = out.readNBytes(40);
//            text = new String(out.readNBytes(160));

                if (no > 0) {
                    int n = out.read(buffer, 0, Math.min(no, buffer.length));
                    text = new String(buffer, 0, Math.min(no, buffer.length));
                    outw.write(text.getBytes(StandardCharsets.UTF_8));
                    outw.flush();
                    buffer = new byte[4000];
                    nuxmvTurn.set(false);
                    while (!nuxmvTurn.get()) {
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

        String agentStateRegex = "(^|\\n)[^=\\n\\^]+\\-state +=[^=\\n$]+(\\n|$)";
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

            String agentVarRegex = "(^|\\n) *(" + agent + ")\\-[^\\n=]+=[^=\\n$]+(\\n|$)";
            Pattern compile2 = Pattern.compile(agentVarRegex, Pattern.MULTILINE);
            Matcher matcher2 = compile2.matcher(nuxmvSimOutput);
            JSONObject vars = new JSONObject();

            while (matcher2.find()){
                String[] group2 = matcher2.toMatchResult().group().split("=");

                String[] left2 = group2[0].split("\\-");

                String var2 = left2[1].trim();
                if(var2.equals("state")){
                    continue;
                }

                String val2 = group2[1].trim().replaceAll(agent + "\\-", "");
            }

            ((JSONObject) jsonObject.get(agent)).put("local", vars);
        }

        return jsonObject;
    }
}