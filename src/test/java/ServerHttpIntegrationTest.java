import flak.App;
import flak.AppFactory;
import flak.Flak;
import org.json.JSONObject;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.Assert.*;

public class ServerHttpIntegrationTest {
    @Test
    public void modelQueryIsDecodedOncePreservingOperators() throws Exception {
        AppFactory factory = Flak.getFactory();
        factory.setLocalAddress(new InetSocketAddress("127.0.0.1", 0));
        App app = factory.createApp();
        Server server = new Server();
        app.scan(server);
        app.start();
        try {
            String model = Files.readString(Path.of(
                    "src/test/resources/recipe/analysis/adversarial/arithmetic-multiplication.rcp"))
                    .replace("count * 2", "count + 2");
            String query = URLEncoder.encode(model, StandardCharsets.UTF_8);
            URI uri = URI.create("http://127.0.0.1:" + app.getServer().getPort()
                    + "/setSystem?script=" + query);
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            JSONObject result = new JSONObject(response.body());
            assertFalse(response.body(), result.has("error"));
            assertTrue(response.body(), result.has("symbolic"));
            assertNotNull(server.system);
        } finally {
            app.getServer().stop();
        }
    }
}
