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

	private final long originalReadTimeoutMs = MediaApi.READ_TIMEOUT_MS;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		baseUrl = "http://localhost:" + server.getAddress().getPort();
		server.start();
	}

	@AfterEach
	void stopServer() {
		MediaApi.READ_TIMEOUT_MS = originalReadTimeoutMs;
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

	// --- Stalled response body (headers arrive, body never completes) ---

	/** Accepts connections, sends headers promising 100 body bytes plus a few, then goes quiet. */
	private static class StallingServer implements AutoCloseable {
		final ServerSocket serverSocket;
		final List<Socket> accepted = Collections.synchronizedList(new ArrayList<>());

		StallingServer() throws IOException {
			serverSocket = new ServerSocket(0);
			Thread t = new Thread(() -> {
				try {
					while (true) {
						Socket socket = serverSocket.accept();
						accepted.add(socket);
						OutputStream out = socket.getOutputStream();
						out.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 100\r\n\r\n{\"tracks\":[".getBytes(StandardCharsets.UTF_8));
						out.flush();
					}
				} catch (IOException e) {
					// server closed
				}
			});
			t.setDaemon(true);
			t.start();
		}

		String url() {
			return "http://127.0.0.1:" + serverSocket.getLocalPort();
		}

		@Override
		public void close() throws IOException {
			serverSocket.close();
			for (Socket s : accepted) s.close();
		}
	}

	private static long openFileDescriptors() throws IOException {
		try (var fds = java.nio.file.Files.list(java.nio.file.Path.of("/proc/self/fd"))) {
			return fds.count();
		}
	}

	@Test
	void stalledResponseBodyThrowsWithinDeadline() throws Exception {
		try (StallingServer stalling = new StallingServer()) {
			MediaApi api = new MediaApi(stalling.url(), "test-key");
			MediaApi.READ_TIMEOUT_MS = 1_000;
			long start = System.nanoTime();
			assertThrows(IOException.class, () -> api.fetchTracks("/v3/tracks/random"));
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			assertTrue(elapsedMs < 5_000, "Stalled request should fail near the deadline, took " + elapsedMs + "ms");
		}
	}

	@Test
	void stalledRequestsDoNotLeakClientSockets() throws Exception {
		try (StallingServer stalling = new StallingServer()) {
			MediaApi api = new MediaApi(stalling.url(), "test-key");
			MediaApi.READ_TIMEOUT_MS = 300;
			int attempts = 10;
			long before = openFileDescriptors();
			for (int i = 0; i < attempts; i++) {
				assertThrows(IOException.class, () -> api.fetchTracks("/v3/tracks/random"));
			}
			Thread.sleep(500);
			long growth = openFileDescriptors() - before;
			// Each attempt adds one server-side socket held by this test; a leaked client socket would double that
			assertTrue(growth <= stalling.accepted.size() + 2, "Client sockets appear to leak: fd growth " + growth + " for " + stalling.accepted.size() + " server-side sockets");
		}
	}
}
