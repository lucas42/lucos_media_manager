import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import com.google.gson.*;

public class MediaApi {
	private Gson gson;
	private final String apiUrl;
	private final String apiKey;
	private final java.net.http.HttpClient httpClient;

	public MediaApi() {
		this(System.getenv("MEDIA_API"), System.getenv("KEY_LUCOS_MEDIA_METADATA_API"));
	}

	/**
	 * Package-private constructor for testing — accepts explicit apiUrl and apiKey
	 * rather than reading from environment variables.
	 */
	MediaApi(String apiUrl, String apiKey) {
		gson = CustomGson.get(this);
		this.apiUrl = apiUrl;
		this.apiKey = apiKey;
		this.httpClient = java.net.http.HttpClient.newBuilder()
			.connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
			.build();
	}

	static final int CONNECT_TIMEOUT_MS = 5_000;
	static long READ_TIMEOUT_MS = 30_000; // not final, so tests can shorten it

	/**
	 * Makes an HTTP request to the given API path.
	 * For GET requests, jsonBody should be null.
	 * Returns null for 204 No Content responses.
	 * The whole exchange (headers and body) must finish within the read timeout.
	 */
	private Reader fetch(String path, Method method, String jsonBody) throws MalformedURLException, IOException {
		java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + path))
			.header("Authorization", "Bearer " + apiKey)
			.timeout(Duration.ofMillis(READ_TIMEOUT_MS));

		if (jsonBody != null) {
			builder.header("Content-Type", "application/json");
			builder.method(method.name(), java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
		} else {
			builder.method(method.name(), java.net.http.HttpRequest.BodyPublishers.noBody());
		}

		java.net.http.HttpResponse<String> response;
		java.util.concurrent.CompletableFuture<java.net.http.HttpResponse<String>> pendingResponse =
			httpClient.sendAsync(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
		try {
			response = pendingResponse.get(READ_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
		} catch (java.util.concurrent.TimeoutException e) {
			pendingResponse.cancel(true);
			throw new IOException("Media API request exceeded " + READ_TIMEOUT_MS + "ms", e);
		} catch (java.util.concurrent.ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) throw (IOException) cause;
			throw new IOException("Media API request failed", cause);
		} catch (InterruptedException e) {
			pendingResponse.cancel(true);
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request interrupted", e);
		}

		int responseCode = response.statusCode();
		if (responseCode == 404) {
			throw new IOException("Not Found");
		} else if (responseCode >= 400) {
			throw new IOException("API error: HTTP " + responseCode);
		}
		if (responseCode == 204) {
			return null;
		}
		return new StringReader(response.body());
	}

	private Reader fetch(String path) throws MalformedURLException, IOException {
		return fetch(path, Method.GET, null);
	}

	public Track fetchTrack(String path) throws MalformedURLException, IOException {
		try {
			return gson.fromJson(fetch(path), Track.class);
		} catch (JsonSyntaxException e) {
			throw new IOException("Malformed JSON from media API: " + e.getMessage(), e);
		}
	}

	public MediaApiResult fetchTracks(String path) throws MalformedURLException, IOException {
		return gson.fromJson(fetch(path), MediaApiResult.class);
	}

	public MediaCollection[] fetchCollections(String path) throws MalformedURLException, IOException {
		return gson.fromJson(fetch(path), MediaCollection[].class);
	}

	/**
	 * Sends a PATCH request to the given API path with a JSON body.
	 * Used to update tag values on tracks (e.g. lastSuccessfulPlay, lastSkip, lastError).
	 */
	public void patch(String path, String jsonBody) throws MalformedURLException, IOException {
		Reader reader = fetch(path, Method.PATCH, jsonBody);
		if (reader != null) {
			reader.close();
		}
	}
}

class MediaApiResult {
	Track[] tracks;
	int totalPages;
}
