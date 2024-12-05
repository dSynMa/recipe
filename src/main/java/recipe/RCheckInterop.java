package recipe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import recipe.lang.System;
import recipe.lang.utils.exceptions.ParsingException;

public class RCheckInterop {
    public static JSONObject parse(Path inputFilePath) throws IOException, InterruptedException, ParsingException {
        ProcessBuilder builder = new ProcessBuilder(Config.rcheckPath, "generate", inputFilePath.toRealPath().toString());
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        JSONTokener jt = new JSONTokener(reader);
        try {
            JSONObject jo = new JSONObject(jt);
            return jo;
        } catch (JSONException ext) {
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder sb = new StringBuilder();
            for (String line; (line = errReader.readLine())!= null; sb.append(line));
            throw new ParsingException(sb.toString());
        } finally {
            process.waitFor();
            process.destroy();
            reader.close();
        }
    }

    public static System parseAndDeserialize(Path inputPath) throws Exception {
        JSONObject jo = parse(inputPath);
        return System.deserialize(jo);
    }

    public static System parseAndDeserialize(String script) throws Exception {
        File tmp = File.createTempFile("rcheck", ".rcp");
        tmp.deleteOnExit();
        FileWriter fw = new FileWriter(tmp);
        fw.write(script);
        fw.close();
        JSONObject jo = RCheckInterop.parse(tmp.toPath());
        tmp.delete();
        return recipe.lang.System.deserialize(jo);
    }

}
