import flak.App;
import flak.AppFactory;
import flak.Flak;
import org.json.JSONObject;
import org.junit.Test;
import recipe.analysis.TranslationTestModels;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

public class ServerReplayIntegrationTest {

    @Test(timeout = 30000)
    public void postReplayReturnsTraceAndFinalState() throws Exception {
        org.junit.Assume.assumeTrue(Files.isExecutable(TranslationTestModels.nuxmvExecutable()));

        String model = Files.readString(Path.of(
            "src/test/resources/recipe/analysis/deserialization/twoway.json"));
        String replay = Files.readString(Path.of(
            "src/test/resources/recipe/analysis/deserialization/twoway.replay.json"));

        AppFactory factory = Flak.getFactory();
        factory.setLocalAddress(new InetSocketAddress("127.0.0.1", 0));
        App app = factory.createApp();
        Server server = new Server();
        server.system = recipe.lang.System.deserialize(new JSONObject(model));
        app.scan(server);
        app.start();
        try {
            URI uri = URI.create("http://127.0.0.1:" + app.getServer().getPort()
                + "/interpretLoadJSON");
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(replay))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(response.body(), 200, response.statusCode());
            JSONObject result = new JSONObject(response.body());
            assertFalse(response.body(), result.has("error"));
            assertEquals(4, result.getJSONArray("trace").length());
            assertTrue(result.has("state"));
            assertTrue(result.getJSONArray("svgs").length() > 0);
        } finally {
            app.getServer().stop();
            Field service = Server.class.getDeclaredField("service");
            service.setAccessible(true);
            ((ExecutorService) service.get(server)).shutdownNow();
        }
    }
}
