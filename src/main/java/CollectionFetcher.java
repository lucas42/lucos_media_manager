import java.io.* ;
import java.net.* ;
import com.google.gson.*;

public class CollectionFetcher extends Fetcher {
	private static Gson gson = CustomGson.get();
	private String slug;


	// Constructor
	public CollectionFetcher(String slug) {
		this.slug = slug;
	}
	
	// Implement the run() method of the Runnable interface.
	@Override
	public void run() {
		System.err.println("DEBUG: Fetching random tracks to add to playlist");
		try {
			fetchList();
		} catch (Exception e) {
			System.err.println("ERROR: Can't fetch new tracks.");
			e.printStackTrace(System.err);
		}
	}
	private void fetchList() throws MalformedURLException, IOException{
		URL url = new URL("https://media-api.l42.eu/v2/collections/" + slug);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
			throw new IOException("Can't fetch more tracks; collection not found "+slug);
		} else if (responseCode >= 400) {
			throw new IOException("API error when fetching more tracks");
		}
		InputStreamReader reader = new InputStreamReader(connection.getInputStream());

		MediaApiResult result = gson.fromJson(reader, MediaApiResult.class);
		playlist.queue(result.tracks);
		System.err.println("DEBUG: New tracks added to playlist from " + slug);
	}

}