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

public class NuXmvSimulation {

    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    Thread process;
    public boolean symbolic = false;
    boolean started = false;
    Path path;

    public NuXmvSimulation(String nuxmvScript) throws IOException {
        Files.delete(Path.of("./forSimulation.smv"));
        path = Files.createFile(Path.of("./forSimulation.smv"));
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

    public Pair<Boolean, String> symbolicModelCheck(String property, int steps) throws IOException {
        String out = execute("go_msat");
        while(out.startsWith("*** This is nuXmv")) out = execute("go_msat");
        if(out.contains("file ")){
            return new Pair<>(false, out);
        }
        out = execute("msat_check_ltlspec_bmc -p \"" + property + "\" -k " + steps);
        out = out.replaceAll("nuXmv > ", "").trim();
        started = true;
        return new Pair<>(true, out);
    }
    public Pair<Boolean, String> initialise() throws IOException {
        String out = execute("go");
        if(out.contains("Impossible to build a BDD FSM")){
            out = execute("go_msat");
            if(!out.contains("file ")){
                symbolic = true;
            }
        }

        if(!out.contains("file ")) {
            return new Pair<>(true, out);
        }

        return new Pair<>(false, out);
    }

    public Pair<Boolean, String> simulation_pick_init_state(String constraint) throws IOException {
        String out = execute((symbolic ? "msat_" : "") + "pick_state -v -c \"" + constraint + "\"");
        if(out.contains("No trace")){
            return new Pair<>(false, out);
        }

        started = true;
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
        if(!started) return simulation_pick_init_state(constraint);

        String out = execute((symbolic ? "msat_" : "") + "simulate -k 1 -v -t \"" + constraint + "\"").replaceAll("(nuXmv >)", "").trim();
        if(symbolic) {
            if (out.contains("UNSAT")) {
                return new Pair<>(false, out.replaceAll("\r\n|\n", " ").split("\\*\\*\\*\\*")[0]);
            } else {
                return new Pair<>(true, parseLastState(out)); //To parse next state
            }
        } else{
            if (out.contains("No future state exists")){
                return new Pair<>(false, out.replaceAll("\r\n|\n", " ").split("\\*\\*\\*\\*")[0]);
            } else {
                return new Pair<>(true, parseLastState(out)); //To parse next state
            }
        }
    }

    public Thread startNuXmvThread() throws IOException {
        Thread t = new Thread(() -> {
            try {
                runNuXmv(path, in, byteArrayOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        t.start();
        //waiting for initialisation
        while(nuxmvTurn.get()){}
        return t;
    }

    static AtomicReference<Boolean> nuxmvTurn = new AtomicReference<>(true);

    public static void runNuXmv(Path path, InputStream inr, OutputStream outw) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("nuxmv", "-int", path.toString());
//        ProcessBuilder builder = new ProcessBuilder("nuxmv", "-source", "commands.txt", "-v", "1", "translation.smv");
        builder.redirectErrorStream(true);
       Process process = builder.start();
//        Process process = Runtime.getRuntime().exec("nuxmv -int " + path.toString());
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
                while(!nuxmvTurn.get()){}
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
            }
            catch (InterruptedException e) {
            }

        }

        System.out.println(process.exitValue());
    }

    public static JSONObject outputToJSON(String nuxmvSimOutput){
        JSONObject jsonObject = new JSONObject();

        if(nuxmvSimOutput.toLowerCase(Locale.ROOT).contains("keep-all = true")){
            jsonObject.put("stuck", true);
            return jsonObject;
        }

        String agentStateRegex = "(^|\\n)[^=\\n\\^]+\\.[^\\n=]+=[^=\\n$]+(\\n|$)";
        Pattern compile = Pattern.compile(agentStateRegex, Pattern.MULTILINE);
        Matcher matcher = compile.matcher(nuxmvSimOutput);

        while(matcher.find()){
            String[] group = matcher.toMatchResult().group().split("=");

            String[] left = group[0].split("\\.");

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

                String val2 = group2[1].trim().replaceAll(agent + "\\-", "");

                vars.put(var2, val2);
            }

            ((JSONObject) jsonObject.get(agent)).put("variables", vars);
        }

        return jsonObject;
    }
}