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
	static final int READ_TIMEOUT_MS = 30_000;

	/**
	 * Makes an HTTP request to the given API path.
	 * For GET requests, jsonBody should be null.
	 * Returns null for 204 No Content responses.
	 */
	private InputStreamReader fetch(String path, Method method, String jsonBody) throws MalformedURLException, IOException {
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

		java.net.http.HttpResponse<InputStream> response;
		try {
			response = httpClient.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofInputStream());
		} catch (InterruptedException e) {
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
		return new InputStreamReader(response.body());
	}

	private InputStreamReader fetch(String path) throws MalformedURLException, IOException {
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
		InputStreamReader reader = fetch(path, Method.PATCH, jsonBody);
		if (reader != null) {
			reader.close();
		}
	}
}

class MediaApiResult {
	Track[] tracks;
	int totalPages;
}
