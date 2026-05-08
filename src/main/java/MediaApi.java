import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;

public class MediaApi {
	private Gson gson;
	private String apiUrl = System.getenv("MEDIA_API");
	private String apiKey = System.getenv("KEY_LUCOS_MEDIA_METADATA_API");

	public MediaApi() {
		gson = CustomGson.get(this);
	}

	static final int CONNECT_TIMEOUT_MS = 5_000;
	static final int READ_TIMEOUT_MS = 30_000;

	private InputStreamReader fetch(String path) throws MalformedURLException, IOException {
		URL url = URI.create(apiUrl + path).toURL();
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
			throw new IOException("Not Found");
		} else if (responseCode >= 400) {
			throw new IOException("API error");
		}
		return new InputStreamReader(connection.getInputStream());

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
		URL url = URI.create(apiUrl + path).toURL();
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestMethod("PATCH");
		connection.setDoOutput(true);
		try (OutputStream os = connection.getOutputStream()) {
			os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
		}
		int responseCode = connection.getResponseCode();
		if (responseCode >= 400) {
			throw new IOException("API error: HTTP " + responseCode);
		}
	}
}

class MediaApiResult {
	Track[] tracks;
	int totalPages;
}