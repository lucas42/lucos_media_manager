import java.io.* ;
import java.net.* ;
import com.google.gson.*;

public class RandomFetcher extends Fetcher {
	private static Gson gson = CustomGson.get();
	
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
		URL url = new URL("https://media-api.l42.eu/tracks/random");
		InputStreamReader reader = new InputStreamReader(url.openStream());

		Track[] tracks = gson.fromJson(reader, Track[].class);
		playlist.queue(tracks);
		System.err.println("DEBUG: New tracks added to playlist");
	}

}