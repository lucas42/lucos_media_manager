import java.io.* ;
import java.net.* ;
import com.google.gson.*;

public class MediaApi {
	private static Gson gson = CustomGson.get();
	private	String apiUrl = System.getenv("MEDIA_API");
	private	String apiKey = System.getenv("KEY_LUCOS_MEDIA_METADATA_API");

	private InputStreamReader fetch(String path) throws MalformedURLException, IOException {
		URL url = new URL(apiUrl+path);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Authorization", "key "+apiKey);
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
			throw new IOException("Not Found");
		} else if (responseCode >= 400) {
			throw new IOException("API error");
		}
		return new InputStreamReader(connection.getInputStream());

	}
	public MediaApiResult fetchTracks(String path) throws MalformedURLException, IOException {
		return gson.fromJson(fetch(path), MediaApiResult.class);
	}
	public MediaCollection[] fetchCollections(String path) throws MalformedURLException, IOException {
		return gson.fromJson(fetch(path), MediaCollection[].class);
	}
}

class MediaApiResult {
	Track[] tracks;
	int totalPages;
}