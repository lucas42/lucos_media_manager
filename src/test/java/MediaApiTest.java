import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class MediaApiTest {

	@Test
	void connectTimeoutIsSet() {
		assertTrue(MediaApi.CONNECT_TIMEOUT_MS > 0, "Connect timeout must be a positive value");
		assertTrue(MediaApi.CONNECT_TIMEOUT_MS <= 10_000, "Connect timeout should be at most 10 seconds");
	}

	@Test
	void readTimeoutIsSet() {
		assertTrue(MediaApi.READ_TIMEOUT_MS > 0, "Read timeout must be a positive value");
		assertTrue(MediaApi.READ_TIMEOUT_MS <= 60_000, "Read timeout should be at most 60 seconds");
	}

	// --- Integration tests using an in-process HttpServer ---

	private HttpServer server;
	private String baseUrl;
	private final List<String> recordedMethods = new ArrayList<>();
	private final List<String> recordedPaths = new ArrayList<>();
	private final List<String> recordedBodies = new ArrayList<>();
	private final List<String> recordedAuthHeaders = new ArrayList<>();

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		baseUrl = "http://localhost:" + server.getAddress().getPort();
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
		recordedMethods.clear();
		recordedPaths.clear();
		recordedBodies.clear();
		recordedAuthHeaders.clear();
	}

	/** Registers a handler that records the incoming request and replies with the given status/body. */
	private void addHandler(String path, int responseCode, String responseBody) {
		server.createContext(path, exchange -> {
			recordedMethods.add(exchange.getRequestMethod());
			recordedPaths.add(exchange.getRequestURI().getPath());
			recordedBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			recordedAuthHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));

			byte[] respBytes = responseBody != null ? responseBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
			exchange.sendResponseHeaders(responseCode, responseBody != null ? respBytes.length : -1);
			if (responseBody != null) {
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(respBytes);
				}
			}
			exchange.close();
		});
	}

	/**
	 * This test would have caught the HttpURLConnection bug before PR #251 merged:
	 * HttpURLConnection silently throws ProtocolException for PATCH; HttpClient sends it fine.
	 */
	@Test
	void patchRequestReachesServer() throws Exception {
		addHandler("/v3/tracks/42", 204, null);

		MediaApi api = new MediaApi(baseUrl, "test-key");
		api.patch("/v3/tracks/42", "{\"tags\":{\"lastSuccessfulPlay\":[]}}");

		assertEquals(1, recordedMethods.size(), "Exactly one request should reach the server");
		assertEquals("PATCH", recordedMethods.get(0), "Request method should be PATCH");
		assertEquals("/v3/tracks/42", recordedPaths.get(0));
	}

	@Test
	void patchSendsJsonBody() throws Exception {
		addHandler("/v3/tracks/99", 204, null);

		MediaApi api = new MediaApi(baseUrl, "test-key");
		String body = "{\"tags\":{\"lastSkip\":[{\"name\":\"2024-01-01T00:00:00Z\"}]}}";
		api.patch("/v3/tracks/99", body);

		assertEquals(body, recordedBodies.get(0));
	}

	@Test
	void patchSendsAuthorizationHeader() throws Exception {
		addHandler("/v3/tracks/77", 204, null);

		MediaApi api = new MediaApi(baseUrl, "secret-key");
		api.patch("/v3/tracks/77", "{}");

		assertEquals("Bearer secret-key", recordedAuthHeaders.get(0));
	}

	@Test
	void patchThrowsOnErrorResponse() {
		addHandler("/v3/tracks/bad", 500, "Internal Server Error");

		MediaApi api = new MediaApi(baseUrl, "test-key");
		assertThrows(IOException.class, () -> api.patch("/v3/tracks/bad", "{}"));
	}

	@Test
	void getRequestReachesServer() throws Exception {
		addHandler("/v3/tracks", 200, "{\"tracks\":[],\"totalPages\":0}");

		MediaApi api = new MediaApi(baseUrl, "test-key");
		MediaApiResult result = api.fetchTracks("/v3/tracks");

		assertNotNull(result);
		assertEquals("GET", recordedMethods.get(0), "Request method should be GET");
	}
}
