package recipe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import recipe.lang.System;
import recipe.lang.utils.exceptions.ParsingException;

public class RCheckInterop {

    private static JSONObject parseStream(InputStream stream, InputStream errorStream) throws ParsingException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        JSONTokener jt = new JSONTokener(reader);
        JSONObject jo = null;
        try {
             jo = new JSONObject(jt);
        } catch (JSONException ext) {
            if (errorStream != null) {
                BufferedReader errReader = new BufferedReader(new InputStreamReader(errorStream));
                StringBuilder sb = new StringBuilder();
                try {
                    for (String line; (line = errReader.readLine())!= null; sb.append(line));
                    errReader.close();
                } catch (IOException e) {}
                throw new ParsingException(sb.toString());
            } else {
                throw new ParsingException(ext.getMessage());
            }
        } finally {
            try {
                reader.close();
            } catch(IOException e) {}
        }
        return jo;
    }

    public static JSONObject parseJson(Path jsonPath) throws ParsingException {
        try {
            InputStream in = new FileInputStream(jsonPath.toFile());
            return parseStream(in, null);
        } catch (FileNotFoundException ext) {
            throw new ParsingException(ext.getMessage());
        }
    }

    public static JSONObject parse(Path inputFilePath) throws IOException, InterruptedException, ParsingException {
        ProcessBuilder builder = new ProcessBuilder(Config.getRcheckPath(), "generate", inputFilePath.toRealPath().toString());
        Process process = builder.start();
        JSONObject jo = null;
        try {
             jo = parseStream(process.getInputStream(), process.getErrorStream());
        } catch (ParsingException ext) {
            throw ext;
        } finally {
            process.waitFor();
            process.destroy();
        }
        return jo;
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
