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
		try {
			fetchTrack();
		} catch (Exception e) {
			System.err.println("Runtime error:");
			e.printStackTrace(System.err);
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
	}

	class SourceTrack {
		String url;
		Map<String, String> tags;
	}
}