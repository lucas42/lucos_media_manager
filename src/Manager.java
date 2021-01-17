import java.io.* ;
import java.net.* ;
import java.util.* ;
import java.math.BigInteger;
public final class Manager {
	private static Map<String, Object> status = new HashMap<String, Object>();
	private static Playlist playlist;
	private final static String[] noupdates = { "isPlaying" };
	private static Loganne loganne;
	public static void main(String argv[]) throws Exception {

		if (System.getenv("PORT") == null) {
			System.err.println("FATAL: No PORT environment variable specified");
			System.exit(1);
		}
		
		// Set the port number.
		int port;
		try{
			port = Integer.parseInt(System.getenv("PORT"));
		} catch (NumberFormatException e) {
			System.err.println("FATAL: Port must be a number");
			System.exit(2);
			return;
		}

		// TODO: Don't post to production loganne host when running locally
		loganne = new Loganne("lucos_media_manager", "https://loganne.l42.eu");
		
		status.put("isPlaying", true);
		status.put("volume", 0.5);
		status.put("openurl", null);

		playlist = new Playlist(new RandomFetcher(), loganne);
		
		// Establish the listen socket.
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("INFO: outgoing data server ready");
    
		// Process HTTP service requests in an infinite loop.
		while (true) {
			// Listen for a TCP connection request.

			Socket clientSocket = serverSocket.accept();
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			// Construct an object to process the HTTP request message.
			HttpRequest request = new HttpRequest( clientSocket );
			// Create a new thread to process the request.
			Thread thread = new Thread(request);
			// Start the thread.
			thread.start();

		}
	
	}
	public static boolean TogglePlayPause() {
		setPlaying(!getPlaying());
		return getPlaying();
	}
	public static void setPlaying(boolean isPlaying) {
		boolean wasPlaying = getPlaying();
		status.put("isPlaying", isPlaying);
		
		// If unpausing, then update timeset in the current track, so nothing is missed
		if (isPlaying && !wasPlaying && playlist != null) {
			Track now = playlist.getCurrentTrack();
			if (now != null) now.timeSetNow();
		}
	}
	public static boolean getPlaying() {
		return (Boolean)status.getOrDefault("isPlaying", true);
	}
	public static void setVolume(float volume) {
		if (volume > 1) volume = 1;
		if (volume < 0) volume = 0;
		status.put("volume", volume);
	}
	public static boolean playlistHasChanged(int oldhashcode) {
		return (playlist.hashCode() != oldhashcode);
	}
	public static boolean summaryHasChanged(int oldhashcode) {
		if (playlist == null) return true;
		return (playlist.hashCode()+status.hashCode() != oldhashcode);
	}
	public static Map<String, Object> getPlaylist() {
		Map<String, Object> output = new HashMap<String, Object>();
		output.put("playlist", playlist);
		output.put("hashcode", playlist.hashCode());
		return output;
	}
	public static Map<String, Object> getFullSummary() {
		Map<String, Object> summary = new HashMap<String, Object>();
		summary.put("tracks", playlist);
		summary.put("volume", status.get("volume"));
		summary.put("isPlaying", status.get("isPlaying"));
		summary.put("devices", Device.getAll());
		if (playlist != null) summary.put("hashcode", playlist.hashCode()+status.hashCode());
		return summary;
	}
	public static Map<String, Object> getBriefSummary() {
		Map<String, Object> summary = new HashMap<String, Object>();
		summary.put("volume", status.get("volume"));
		summary.put("isPlaying", status.get("isPlaying"));
		summary.put("now", playlist.getCurrentTrack());
		summary.put("next", playlist.getNextTrack());
		if (playlist != null) summary.put("hashcode", playlist.hashCode()+status.hashCode());
		return summary;
	}
	public static void update(Map<String, String> changes) {
		
		// Remove any keys which shouldn't be overwritten
		for (String key : noupdates) changes.remove(key);
		status.putAll(changes);
	}
	public static void queueNow(Track track) {
		playlist.queueNow(track);
	}
	public static void queueNext(Track track) {
		playlist.queueNext(track);
	}
	public static void queueEnd(Track track) {
		playlist.queueEnd(track);
	}
	public static void queuem3u(BufferedReader br) throws IOException {
		queuem3u(br, "");
	}
	public static void queuem3u(BufferedReader br, String prefix) throws IOException {
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.charAt(0) == '#' || line.indexOf('/') == -1) continue;
			queueEnd(new Track(prefix+line));
		}
	}
	public static void next() {
		playlist.next();
	}
	private static void openUrl(String type, String url) {
		Map<String, String> openUrl = new HashMap<String, String>();
		openUrl.put("type", type);
		openUrl.put("url", url);
		status.put("open", openUrl);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			System.err.println("ERROR: Thread Sleep interrupted:");
			e.printStackTrace(System.err);
		}
		if (status.get("open").hashCode() == openUrl.hashCode()) status.put("open", null);
	}
	public static boolean openExtUrl() {
			Track now = playlist.getCurrentTrack();
			if (now == null) return false;
			String exturl = now.getMetadata("exturl");
			if (exturl == null) return false;
			openUrl("ext", exturl);
			return true;
	}
	public static boolean openEditUrl() {
			Track now = playlist.getCurrentTrack();
			if (now == null) return false;
			String editurl = now.getMetadata("editurl");
			if (editurl == null) return false;
			openUrl("edit", editurl);
			return true;
	}
	public static void finished(Track oldtrack, String trackstatus) {
		playlist.finished(oldtrack, trackstatus);
	}
	public static boolean update(Track curtrack, float currentTime, BigInteger currentTimeSet) {
		return playlist.setTrackTime(curtrack, currentTime, currentTimeSet);
	}
	public static boolean isCurrentURL(String url) {
		Track now = playlist.getCurrentTrack();
		if (now == null) return false;
		return now.getUrl().equals(url);
	}
	public static int getPlaylistLength() {
		return playlist.getLength();
	}
}
