import org.apache.commons.cli.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.petitparser.context.ParseError;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.analysis.NuXmvInteraction;
import recipe.analysis.ToNuXmv;
import recipe.lang.utils.Pair;

public class CLIApp
{
    public static void main( String[] args ) throws Exception
    {
        Options options = new Options();

        Option input = new Option("i", "input", false, "info: input recipe script file\nargs: <recipe script>");
        Option nuxmv = new Option("n", "smv", false, "info: output to smv file");
        Option dot = new Option("d", "dot", false, "info: output agents DOT files");
        Option mc = new Option("mc", "mc", false, "info: model checks input script file");
        Option simulation = new Option("sim", "simulate", false, "info: opens file in simulation mode");
        Option server = new Option("s", "server", true, "info: open server on given port\nargs: <port>");
        Option frontend = new Option("f", "frontend", true, "info: opens front end and server on given ports\nargs: <server-port>,<frontend-port>");

        input.setRequired(true);

        options.addOption(input);
        options.addOption(nuxmv);
        options.addOption(dot);
        options.addOption(mc);
        options.addOption(simulation);
        options.addOption(server);
        options.addOption(frontend);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;//not a good practice, it serves it purpose

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("recipe", options);

            System.exit(1);
        }

        if(cmd.hasOption("f")){
            String port = cmd.getOptionValue("f");
            if(!port.matches(" *[0-9][0-9][0-9][0-9] *, *[0-9][0-9][0-9][0-9] *")){
                System.out.println("-f option must be accompanied with two port numbers, i.e. an argument of the form \"8082,8083\"");
            }
            String[] fargs = port.split(" *, *");
            Server.start(fargs[0]);
            System.out.println("Launched server on http://localhost:" + fargs[0]);

            Process exec = Runtime.getRuntime().exec("python3 ./frontend/launch.py " + fargs[1] + " http://localhost:" + fargs[0]);
            System.out.println("Launched frontend on http://localhost:" + fargs[1]);

            exec.getInputStream().transferTo(System.out);
        }
        else if(cmd.hasOption("s")){
            String port = cmd.getOptionValue("s");
            Server.start(port);
            Server.app().wait();
            System.exit(1);
        } else if(!cmd.hasOption("i")){
            formatter.printHelp("recipe", options);

            System.exit(1);
        }

        Path inputFilePath = Path.of(cmd.getOptionValue("input"));

        String script = String.join("\n", Files.readAllLines(inputFilePath));

        String transform = "";
        recipe.lang.System system = null;
        try {
            Parser systemParser = recipe.lang.System.parser().end();
            Result r = systemParser.parse(script);
            if(r.isFailure()){
                System.out.println(r.getMessage());
                System.out.println(r.getPosition());
                System.out.println("Could not parse the following: \n" + script.substring(r.getPosition()));
                return;
            }
            system = r.get();

            transform = ToNuXmv.transform(system);
        } catch (Exception e){
            System.out.println(e.getMessage());
            System.exit(1);
        }

        if(cmd.hasOption("smv")){
            String name = inputFilePath.getFileName().toString().split("\\.")[0] + ".smv";
            Files.write(Path.of(name), transform.getBytes(StandardCharsets.UTF_8));
        }
        if(cmd.hasOption("dot")){
            String name = inputFilePath.getFileName().toString().split("\\.")[0] + ".dot";
            Files.write(Path.of(name), system.toDOT());
        }
        NuXmvInteraction nuXmvInteraction = null;
        if(cmd.hasOption("mc")){
            try {
                if(system.getLtlspec() == null || system.getLtlspec().size() == 0){
                    System.out.println("No specifications to model check.");
                } else{
                    String out = ToNuXmv.nuxmvModelChecking(system);
                    if(out.equals("") || system.isSymbolic()){
                        nuXmvInteraction = new NuXmvInteraction(system);
                        for(int i = 0; i < system.getLtlspec().size(); i++) {
                            String spec = system.getLtlspec().get(i).replaceAll("^ *[^ ]+ +", "");
                            int bound = 0;
                            if(system.isSymbolic()){
                                System.out.println("Specification is symbolic, thus bounded model checking will be used. Please specify an integer bound: ");
                                Scanner scanner = new Scanner(System.in);
                                bound = scanner.nextInt();
                            }
                            Pair<Boolean, String> result = nuXmvInteraction.modelCheck(spec, bound);
                            if(result.getLeft()) {
                                out += spec + ":\n" + result.getRight() + "\n";
                            } else{
                                out += spec + " (error) :\n" + result.getRight() + "\n";
                            }
                        }
                        nuXmvInteraction.stopNuXmvThread();
                    }
                    System.out.println(out);
                }
            } catch (ParseError parseError){
                System.out.println(parseError.getFailure().toString());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        if(cmd.hasOption("sim")){
            if(nuXmvInteraction == null){
                nuXmvInteraction = new NuXmvInteraction(system);
            }
            Pair<Boolean, String> initialise = nuXmvInteraction.initialise();
            if(!initialise.getLeft()){
                System.out.println(initialise.getRight());
                return;
            }
            boolean exit = false;
            Scanner scanner = new Scanner(System.in);
            System.out.println("You have started simulation interactive mode, exit by pressing <ctrl+c>.");
            System.out.println("Write any constraint you wish of the initial state and press <enter> to continue.");
            String constraint = scanner.next();
            Pair<Boolean, String> result = nuXmvInteraction.simulation_pick_init_state(constraint);
            if(!result.getLeft()){
                System.out.println(initialise.getRight());
                return;
            }

            while (!exit){
                System.out.println("Write any constraint you wish of the next state and press <enter> to continue.");
                constraint = scanner.next();
                result = nuXmvInteraction.simulation_next(constraint);
                System.out.println(initialise.getRight());
                if(!result.getLeft()){
                    return;
                }
            }
        }

    }
}
