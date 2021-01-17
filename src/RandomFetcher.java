import java.io.* ;
import java.net.* ;
import java.util.* ;
import com.google.gson.*;
import java.lang.reflect.Type;
public class RandomFetcher extends Fetcher {
	private static Gson gson = customGson();
	
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

	/**
	 * Returns a Gson object with a custom deserializer for handling tracks from the media api
	 **/
	private static Gson customGson() {
		GsonBuilder gsonBuilder = new GsonBuilder();

		JsonDeserializer<Track> trackDeserializer =
			new JsonDeserializer<Track>() {
				@Override
				public Track deserialize(JsonElement json, Type typeOfSrc, JsonDeserializationContext context) {

					String url = json.getAsJsonObject().get("url").getAsString();
					Map<String, String> metadata = context.deserialize(json.getAsJsonObject().get("tags"), Map.class);
					return new Track(url, metadata);
				}
			};
		gsonBuilder.registerTypeAdapter(Track.class, trackDeserializer);
		return gsonBuilder.create();
	}

}