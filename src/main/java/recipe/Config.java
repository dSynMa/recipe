package recipe;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public static final String channelLabel = "channel";
    public static final String broadcast = "*";

    public static String getNuxmvPath() {
        String nuxmvPath = "";
        if(Files.exists(Path.of("./nuxmv/bin/nuxmv"))||Files.exists(Path.of("./nuxmv/bin/nuxmv.exe"))) {
            nuxmvPath = "./nuxmv/bin/nuxmv";
        } else if(Files.exists(Path.of("./bin/nuxmv"))||Files.exists(Path.of("./bin/nuxmv.exe"))) {
            nuxmvPath = "./bin/nuxmv";
        } else {
            nuxmvPath = "nuxmv";
        }

        return nuxmvPath;
    }
}

