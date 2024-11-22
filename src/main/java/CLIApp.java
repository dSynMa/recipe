import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.petitparser.context.ParseError;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.analysis.NuXmvInteraction;
import recipe.analysis.ToNuXmv;
import recipe.lang.utils.Pair;

public class CLIApp {
    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    static {
        try {
            InputStream properties = CLIApp.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration(properties);
        } catch (IOException e) {
            Logger.getGlobal().severe(e.toString());
        }
    }

    static String npm = isWindows() ? "npm.cmd" : "npm";

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option input = new Option("i", "input", true, "info: input recipe script file\nargs: <recipe script>");
        Option nuxmv = new Option("n", "smv", false, "info: output to smv file");
        Option dot = new Option("d", "dot", false, "info: output agents DOT files");
        Option mc = new Option("mc", "mc", false, "info: model checks input script file");
        Option bmc = new Option("bmc", "bmc", true,
                "info: bounded model checks input script file\nargs: bound (by default 10)");
        Option simulation = new Option("sim", "simulate", false, "info: opens file in simulation mode");
        Option gui = new Option("g", "gui", false, "info: opens gui");
        Option threads = new Option("t", "threads", true,
                "info: how many threads to use in model-checking (gui only, default=num of hw threads)");
        Option port = new Option("p", "port", true, "info: port the GUI server will listen to (default 3000)");

        options.addOption(input);
        options.addOption(nuxmv);
        options.addOption(dot);
        options.addOption(mc);
        options.addOption(bmc);
        options.addOption(simulation);
        options.addOption(gui);
        options.addOption(threads);
        options.addOption(port);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("gui")) {
                runGui(cmd);
            } else if (!cmd.hasOption("i")) {
                formatter.printHelp("recipe", options);
                System.exit(1);
            } else {
                runCli(cmd);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("recipe", options);
            System.exit(1);
        }
    }

    private static void runCli(CommandLine cmd) throws Exception, IOException {
        Path inputFilePath = Path.of(cmd.getOptionValue("i"));
        String script = String.join("\n", Files.readAllLines(inputFilePath));
        String transform = "";
        recipe.lang.System system = null;
        try {
            Parser systemParser = recipe.lang.System.parser().end();
            Result r = systemParser.parse(script);
            if (r.isFailure()) {
                System.out.println(r.getMessage());
                System.out.println(r.getPosition());
                System.out.println("Could not parse the following: \n" + script.substring(r.getPosition()));
                return;
            }
            system = r.get();

            transform = ToNuXmv.transform(system);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption("smv")) {
            String name = inputFilePath.getFileName().toString().split("\\.")[0] + ".smv";
            Files.write(Path.of(name), transform.getBytes(StandardCharsets.UTF_8));
        }
        if (cmd.hasOption("dot")) {
            String filename = inputFilePath.getFileName().toString().split("\\.")[0];
            if (!Files.exists(Path.of(filename))) {
                Files.createDirectory(Path.of(filename));
            }
            JSONArray json = new JSONArray(system.toDOT());
            for (int i = 0; i < json.length(); i++) {
                String agentName = new JSONObject(json.getString(i)).getString("name");
                String agentDot = new JSONObject(json.getString(i)).getString("graph");
                Files.write(Path.of(filename + "/" + agentName + ".dot"), agentDot.getBytes(StandardCharsets.UTF_8));
            }
        }
        NuXmvInteraction nuXmvInteraction = null;
        if (cmd.hasOption("mc")) {
            try {
                if (system.getSpecs() == null || system.getSpecs().size() == 0) {
                    System.out.println("No specifications to model check.");
                } else {
                    String out = ToNuXmv.nuxmvModelChecking(system);
                    if (out.equals("") || system.isSymbolic()) {
                        nuXmvInteraction = new NuXmvInteraction(system);
                        for (int i = 0; i < system.getSpecs().size(); i++) {
                            String spec = system.getSpecs().get(i).toString().replaceAll("^ *[^ ]+ +", "");
                            if (!system.getSpecs().get(i).isPureLTL()) {
                                System.out.println(
                                        "LTOL model checking not supported yet, skipping model checking of:\n" + spec);
                                continue;
                            }
                            int bound = 0;
                            if (system.isSymbolic()) {
                                System.out.println(
                                        "There is a symbolic specification, and thus bounded model checking will be used. Please specify an integer bound: ");
                                Scanner scanner = new Scanner(System.in);
                                bound = scanner.nextInt();
                                scanner.close();
                            }
                            Pair<Boolean, String> result = nuXmvInteraction.modelCheck(spec, false, bound);
                            if (result.getLeft()) {
                                out += spec + ":\n" + result.getRight() + "\n";
                            } else {
                                out += spec + " (error) :\n" + result.getRight() + "\n";
                            }
                        }
                        nuXmvInteraction.stopNuXmvThread();
                    }
                    System.out.println(out);
                }
            } catch (ParseError parseError) {
                System.out.println(parseError.getFailure().toString());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        if (cmd.hasOption("bmc")) {
            try {
                if (system.getSpecs() == null || system.getSpecs().size() == 0) {
                    System.out.println("No specifications to model check.");
                } else {
                    String out = "";
                    nuXmvInteraction = new NuXmvInteraction(system);
                    for (int i = 0; i < system.getSpecs().size(); i++) {
                        String spec = system.getSpecs().get(i).toString().replaceAll("^ *[^ ]+ +", "");
                        if (!system.getSpecs().get(i).isPureLTL()) {
                            System.out.println(
                                    "LTOL model checking not supported yet, skipping model checking of:\n" + spec);
                            continue;
                        }
                        int bound = 10;
                        try {
                            bound = Integer.parseInt(cmd.getOptionValue("bmc"));
                        } catch (Exception e) {
                            System.out
                                    .println(cmd.getOptionValue("bmc") + " is not a valid bound. Using a bound of 10.");
                        }

                        Pair<Boolean, String> result = nuXmvInteraction.modelCheck(spec, true, bound);
                        if (result.getLeft()) {
                            out += spec + ":\n" + result.getRight() + "\n";
                        } else {
                            out += spec + " (error) :\n" + result.getRight() + "\n";
                        }
                    }
                    nuXmvInteraction.stopNuXmvThread();
                    System.out.println(out);
                }
            } catch (ParseError parseError) {
                System.out.println(parseError.getFailure().toString());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        if (cmd.hasOption("sim")) {
            if (nuXmvInteraction == null) {
                nuXmvInteraction = new NuXmvInteraction(system);
            }

            boolean exit = false;
            Scanner scanner = new Scanner(System.in);
            System.out.println("You have started simulation interactive mode, exit by pressing <ctrl+c>.");
            System.out.println("Write any constraint you wish of the initial state and press <enter> to continue.");
            String constraint = scanner.next();
            Pair<Boolean, String> result = nuXmvInteraction.simulation_pick_init_state(constraint);
            if (!result.getLeft()) {
                System.out.println(result.getRight());
                scanner.close();
                return;
            }

            while (!exit) {
                System.out.println("Write any constraint you wish of the next state and press <enter> to continue.");
                constraint = scanner.next();
                result = nuXmvInteraction.simulation_next(constraint);
                System.out.println(result.getRight());
                if (!result.getLeft()) {
                    scanner.close();
                    return;
                }
            }
            scanner.close();
        }
    }

    private static void runGui(CommandLine cmd) throws Exception, IOException {
        int numThreads = Runtime.getRuntime().availableProcessors();
        int serverPort = 3000;
        if (cmd.hasOption("threads")) {
            String cliThreads = cmd.getOptionValue("threads");
            try {
                numThreads = Integer.parseInt(cliThreads);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                numThreads = Runtime.getRuntime().availableProcessors();
                System.err.printf("[WARNING] failed to parse --threads %s, will use %d%n", cliThreads, numThreads);
            }
        }
        if (cmd.hasOption("port")) {
            String cliPort = cmd.getOptionValue("port");
            try {
                serverPort = Integer.parseInt(cliPort);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                serverPort = 3000;
                System.err.printf("[WARNING] failed to parse --port %s, will use %d%n", cliPort, serverPort);
            }
        }

        Server.start(numThreads);
        ProcessBuilder processBuilder = new ProcessBuilder();
        File dir = new File(System.getProperty("user.dir") + File.separator + "frontend-react");

        processBuilder.directory(dir);
        if (serverPort != 3000) {
            processBuilder.environment().put("PORT", String.valueOf(serverPort));
        }
        processBuilder.command(npm, "start");
        Process exec = processBuilder.start();

        exec.getInputStream().transferTo(System.out);
    }
}
