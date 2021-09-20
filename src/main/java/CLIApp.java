import org.apache.commons.cli.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.analysis.ToNuXmv;

public class CLIApp
{
    public static void main( String[] args ) throws Exception
    {
        Options options = new Options();

        Option input = new Option("i", "input", true, "recipe script file");
        Option nuxmv = new Option("n", "nuxmv", false, "model check nuxmv");
        Option dot = new Option("d", "dot", false, "output agents DOT files");
        input.setRequired(true);
        nuxmv.setRequired(false);
        options.addOption(input);
        options.addOption(nuxmv);
        options.addOption(dot);

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

        String inputFilePath = cmd.getOptionValue("input");

        String script = String.join("\n", Files.readAllLines(Path.of(inputFilePath)));

        Parser system = recipe.lang.System.parser().end();
        Result r = system.parse(script);
        recipe.lang.System s = r.get();

        if(cmd.hasOption("nuxmv")){
            ToNuXmv.nuxmvModelChecking(s);
        } else{
            String transform = ToNuXmv.transform(s);
            java.lang.System.out.println(transform);
        }

        if(cmd.hasOption("dot")){
            java.lang.System.out.println(s.toDOT());
        }
    }
}
