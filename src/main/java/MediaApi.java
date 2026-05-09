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

	/**
	 * Makes an HTTP request to the given API path.
	 * For GET requests, jsonBody should be null.
	 * Returns null for 204 No Content responses.
	 */
	private InputStreamReader fetch(String path, Method method, String jsonBody) throws MalformedURLException, IOException {
		URL url = URI.create(apiUrl + path).toURL();
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setRequestMethod(method.name());
		if (jsonBody != null) {
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			try (OutputStream os = connection.getOutputStream()) {
				os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
			}
		}
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
			throw new IOException("Not Found");
		} else if (responseCode >= 400) {
			throw new IOException("API error: HTTP " + responseCode);
		}
		if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
			return null;
		}
		return new InputStreamReader(connection.getInputStream());
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
