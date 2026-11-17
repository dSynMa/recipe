import flak.AppFactory;
import flak.Flak;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ServerBuildCancellationTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private final HttpClient client = HttpClient.newHttpClient();
    private String base;

    private HttpRequest request(String route) {
        return HttpRequest.newBuilder(URI.create(base + route)).timeout(Duration.ofSeconds(10)).build();
    }

    private JSONObject get(String route) throws Exception {
        var response = client.send(request(route), HttpResponse.BodyHandlers.ofString());
        assertEquals(response.body(), 200, response.statusCode());
        return new JSONObject(response.body());
    }

    @Test(timeout = 30000)
    public void cancellingInitialisationStopsTheCheckerAndAllowsANewBuild() throws Exception {
        Path dir = temp.getRoot().toPath();
        Path binary = dir.resolve("nuxmv/bin/nuxmv");
        Files.createDirectories(binary.getParent());
        Files.writeString(binary, "#!/bin/sh\nprintf 'nuXmv >'\nread -r command\n"
                + "if [ ! -f retry ]; then echo $$ > checker.pid; exec sleep 30; fi\n"
                + "printf 'nuXmv >'\nwhile read -r command; do printf 'nuXmv >'; done\n");
        assertTrue(binary.toFile().setExecutable(true));
        String classpath = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(path -> Path.of(path).toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator));
        Process server = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-cp", classpath, getClass().getName()).directory(dir.toFile()).redirectErrorStream(true)
                .redirectOutput(dir.resolve("server.log").toFile()).start();
        try {
            base = "http://127.0.0.1:" + awaitFile(dir.resolve("port"));
            String script = Files.readString(Path.of("src/test/resources/recipe/analysis/adversarial/supplier-locality.rcp"));
            String build = "/buildModel?symbolic=true&script=" + URLEncoder.encode(script, StandardCharsets.UTF_8) + "&buildId=";
            String first = get("/prepareBuild").getString("buildId");
            var pending = client.sendAsync(request(build + first), HttpResponse.BodyHandlers.ofString());
            long pid = Long.parseLong(awaitFile(dir.resolve("checker.pid")));
            get("/cancelBuild?buildId=" + first);
            assertTrue(new JSONObject(pending.get(5, TimeUnit.SECONDS).body()).has("error"));
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
            assertTrue(get("/toDOT").has("error"));

            Files.createFile(dir.resolve("retry"));
            String next = get("/prepareBuild").getString("buildId");
            assertTrue(get(build + next).getBoolean("success"));
            get("/cancelBuild?buildId=" + first);
            assertTrue(get("/toDOT").has("agents"));
        } finally {
            server.descendants().forEach(ProcessHandle::destroyForcibly);
            server.destroyForcibly().waitFor();
        }
    }

    private static String awaitFile(Path path) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
        while (System.nanoTime() < deadline) {
            if (Files.exists(path)) {
                String value = Files.readString(path).trim();
                if (!value.isEmpty()) return value;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out waiting for " + path);
    }

    public static void main(String[] args) throws Exception {
        AppFactory factory = Flak.getFactory();
        factory.setLocalAddress(new InetSocketAddress("127.0.0.1", 0));
        var app = factory.createApp();
        app.scan(new Server());
        app.start();
        Files.writeString(Path.of("port"), String.valueOf(app.getServer().getPort()));
    }
}
