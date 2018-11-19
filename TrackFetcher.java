import java.io.* ;
import java.net.* ;
import java.util.* ;
import java.lang.* ;
import com.google.gson.*;
final class TrackFetcher implements Runnable
{
	final static String CRLF = "\r\n";
	// Constructor
	public TrackFetcher() { 
	}
	
	// Implement the run() method of the Runnable interface.
	@Override
	public void run() {
		System.err.println("DEBUG: Fetching more tracks to add to playlist");
		try {
			fetchTrack();
		} catch (Exception e) {
			System.err.println("ERROR: Can't fetch new tracks. "+e.getMessage());
		}
	}
	private void fetchTrack() throws Exception {
		getJSON(Manager.getSetting("playlist"));
	}
	private void getJSON(String listurl) throws MalformedURLException, IOException{
        URL url = new URL(listurl);
        InputStreamReader reader = new InputStreamReader(url.openStream());
        SourceTrack[] tracks = new Gson().fromJson(reader, SourceTrack[].class);
		for (SourceTrack source: tracks) {
			Track track = new Track(source.url, source.tags);
			Manager.queue(track);
		}
		System.err.println("DEBUG: New tracks added to playlist");
	}

	class SourceTrack {
		String url;
		Map<String, String> tags;
	}
}