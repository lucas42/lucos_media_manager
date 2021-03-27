import java.io.* ;
import java.net.* ;
import java.util.* ;
import java.math.BigInteger;
public final class Manager {
	private static Playlist playlist;
	private static DeviceList deviceList = new DeviceList(null);
	private static ConnectionTracker connections = new ConnectionTracker();
	private final static String[] noupdates = { "isPlaying" };
	private static Loganne loganne;
	private static Status status = new Status(null, null, null);
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


		playlist = new Playlist(new RandomFetcher(), loganne);

		// TODO: Keep state of device list between restarts
		deviceList = new DeviceList(loganne);
		
		status = new Status(playlist, connections, deviceList);

		// Establish the listen socket.
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("INFO: outgoing data server ready");
    
		// Process HTTP service requests in an infinite loop.
		while (true) {
			// Listen for a TCP connection request.

			Socket clientSocket = serverSocket.accept();
			// Construct an object to process the HTTP request message.
			HttpRequest request = new HttpRequest( clientSocket );
			// Create a new thread to process the request.
			Thread thread = new Thread(request);
			// Start the thread.
			thread.start();

		}
	
	}
	public static void setPlaying(boolean isPlaying) {
		status.setPlaying(isPlaying);
	}
	public static boolean getPlaying() {
		return status.getPlaying();
	}
	public static void setVolume(float volume) {
		status.setVolume(volume);
	}
	public static boolean summaryHasChanged(int oldhashcode) {
		if (playlist == null) return true;
		return (status.hashCode() != oldhashcode);
	}
	public static Map<String, Object> getFullSummary() {
		Map<String, Object> summary = new HashMap<String, Object>();
		summary.put("tracks", playlist);
		summary.put("volume", status.getVolume());
		summary.put("isPlaying", status.getPlaying());
		summary.put("devices", deviceList.getAllDevices());
		if (playlist != null) summary.put("hashcode", status.hashCode());
		return summary;
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
	public static Loganne getLoganne() {
		return loganne;
	}
	public static void updateDevice(String uuid, String name) {
		Device device = deviceList.getDevice(uuid);
		if (name != null) {
			device.setName(name);
		}
	}
	public static void setCurrentDevice(String uuid) {
		deviceList.setCurrent(uuid);
	}
	public static void openConnection(String device_uuid, HttpRequest request) {
		Device device = deviceList.getDevice(device_uuid);
		connections.open(device, request);
	}
	public static void closeConnection(HttpRequest request) {
		connections.close(request);
	}
	public static boolean isConnected(Device device) {
		return connections.isConnected(device);
	}
}
