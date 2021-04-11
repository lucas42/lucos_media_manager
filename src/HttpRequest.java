import java.io.* ;
import java.net.* ;
import java.util.* ; 
import java.lang.* ; 
import com.google.gson.*;
import java.math.BigInteger;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.net.SocketException;
import java.lang.reflect.Type;
class HttpRequest implements Runnable {
	final static String CRLF = "\r\n";
	private Socket socket;
	private String fullHostname;
	private String hostname;
	private DataOutputStream os;
	private OutputStreamWriter osw;
	private Map<String, String> header = new HashMap<String, String>();
	private Map<String, String> get = new HashMap<String, String>();
	private Gson gson;
	private Status status;
	
	// Constructor
	public HttpRequest(Status status, Socket socket) throws Exception {
		this.status = status;
		this.socket = socket;
		fullHostname = socket.getInetAddress().getHostName();
		this.gson = CustomGson.get(status);
	}
	
	// Implement the run() method of the Runnable interface.
	public void run() {
		try {
			processRequest();

		} catch (java.net.SocketException e) {
			System.out.println("WARNING: Problem with socket on incoming HTTP Request. "+e.getMessage());
		} catch (Exception e) {
			System.err.println("ERROR: Uknown Error (HttpRequest, host:"+fullHostname+"):");
			e.printStackTrace();
		}

		try {
			// Wait 3 seconds before marking this connection as closed, to give the client time to re-establish a long poll
			Thread.sleep(3000);
		} catch (InterruptedException e) {}
		status.getDeviceList().closeConnection(this);
	}
	protected void processRequest() throws Exception {
	
		// Get a reference to the socket's input and output streams.
		InputStream is = new DataInputStream(socket.getInputStream());
		os = new DataOutputStream(socket.getOutputStream());
		osw = new OutputStreamWriter(os, "UTF8");
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF8"));

		String requestLine = br.readLine();

		if (requestLine == null) {
			System.err.println("WARNING: Incoming HTTP Request interrupted before receiving headers.  Aborting response.");
			return;
		}

		// Get the header lines.
		String headerLine = null;
		while (br.ready()) {
			headerLine = br.readLine();
			if (headerLine.length() < 1) break;
			int jj = headerLine.indexOf(':');
			String field;
			if ( jj > -1) {
				field = headerLine.substring(jj+1);
				headerLine = headerLine.substring(0, jj);
			} else {
				field = "true";
			}
			header.put(headerLine, field);
		}
		
		// Extract the filename from the request line.
		StringTokenizer tokens = new StringTokenizer(requestLine);
		String method = tokens.nextToken();
		boolean head = method.equalsIgnoreCase("HEAD");
		boolean post = method.equalsIgnoreCase("POST");
		boolean put = method.equalsIgnoreCase("PUT");
		String path = tokens.nextToken().trim();
		
		String data = null;
		if (header.containsKey("Content-Length")) {
			try {
				int length = Integer.parseInt(header.get("Content-Length").trim());
				char[] cbuf = new char[length];
				br.read(cbuf, 0, length);
				data = new String(cbuf);
			} catch (Exception e) {
				System.err.println("WARNING: Can't get HTTP data from request:");
				e.printStackTrace(System.err);
			}
		}
		String fullpath = path;
		int ii = path.indexOf('?');
		if (ii > -1) {
			String[] getstring = path.substring(ii+1).split("&");
			for (String key : getstring) {
				int jj = key.indexOf('=');
				String field;
				if ( jj > -1) {
					field = key.substring(jj+1);
					key = key.substring(0, jj);
				} else {
					field = "true";
				}
				key = URLDecoder.decode(key, "UTF-8");
				field = URLDecoder.decode(field, "UTF-8");
				get.put(key, field);
			}
			path = path.substring(0, ii);
		}
		String update_url = get.remove("update_url");
		String update_time = get.remove("update_time");
		String update_timeset = get.remove("update_timeset");
		boolean update_success = false;
		if (update_url != null && update_time != null) {
			try{
				Track update_curtrack = new Track(update_url);
				float update_currentTime = Float.parseFloat(update_time);
				BigInteger update_currentTimeSet = new BigInteger(update_timeset);
				update_success = status.getPlaylist().setTrackTime(update_curtrack, update_currentTime, update_currentTimeSet);
			} catch (Exception e) {
				System.err.println("ERROR: Unexpected error updating manager");
				e.printStackTrace();
			}
		}
		String device_uuid = get.remove("device");
		if (device_uuid != null) {
			status.getDeviceList().openConnection(device_uuid, this);
		}
		if (path.equals("/poll/summary")) {
			long startTime = System.nanoTime();
			int hashcode;
			try {
				hashcode = Integer.parseInt(get.get("hashcode"));
			} catch (NumberFormatException e) {
				hashcode = 0;
			}
			while (true) {
				if (status.summaryHasChanged(hashcode)) {
					sendHeaders(200, "Long Poll", "application/json");
					if (!head) osw.write(gson.toJson(status.getSummary()));   
					break;
				}
				if ((System.nanoTime() - startTime) > (1000000000L * 30)) {
					sendHeaders(200, "Long Poll", "application/json");
					if (!head) osw.write("{ }");
					break;
				}
				
				Thread.sleep(1);
			}
		} else if (path.equals("/next") && post) {
			String expectedNowUrl = get.get("now");
			Track actualNow = status.getPlaylist().getCurrentTrack();
			String actualNowUrl = (actualNow == null) ? null : actualNow.getUrl();
			if (expectedNowUrl == null || expectedNowUrl.equals(actualNowUrl)) {
				status.getPlaylist().next();
				sendHeaders(204, "Changed", "application/json");
			} else {
				sendHeaders(204, "Wrong current track", "application/json");
			}
		} else if (path.equals("/play") && post) {
			status.setPlaying(true);
			sendHeaders(204, "Changed", "application/json");
		} else if (path.equals("/pause") && post) {
			status.setPlaying(false);
			sendHeaders(204, "Changed", "application/json");
		} else if (path.equals("/volume") && post) {
			float volume = getFloat("volume", head);
			if (volume != -1) {
				status.setVolume(volume);
				sendHeaders(204, "Changed", "application/json");
			}
		} else if (path.equals("/done") && post) {
			Track oldtrack = new Track(get.get("track"));
			String doneStatus = get.get("status");
			status.getPlaylist().finished(oldtrack, doneStatus);
			sendHeaders(204, "Changed", "application/json");
		} else if (path.equals("/update") && post) {
			
			// All the work has already been done, so just return headers based on result.
			if (update_success) sendHeaders(204, "Changed", "application/json");
			else sendHeaders(400, "Incorrect params", "application/json");
		} else if (path.equals("/queue") && post) {
			HashMap<String, String> metadata = new HashMap<String, String>(get);
			String url = metadata.remove("url");
			String pos = metadata.remove("pos");
			Track newTrack = new Track(url, metadata);
			if (pos.equals("now")) {
				status.getPlaylist().queueNow(newTrack);
			} else if (pos.equals("next")) {
				status.getPlaylist().queueNext(newTrack);
			} else {
				status.getPlaylist().queueEnd(newTrack);
			}
			sendHeaders(204, "Queued", "application/json");
		} else if (path.equals("/devices") && post) {
			status.getDeviceList().updateDevice(get.get("uuid"), get.get("name"));
			sendHeaders(204, "Changed", "text/plain");
		} else if (path.equals("/devices/current") && post) {
			status.getDeviceList().setCurrent(get.get("uuid"));
			sendHeaders(204, "Changed", "text/plain");
		} else if(path.equals("/_info")) {
			Map<String, Object> output = new HashMap<String, Object>();
			Map<String, Map<String, Object>> checks = new HashMap<String, Map<String, Object>>();
			Map<String, Map<String, Object>> metrics = new HashMap<String, Map<String, Object>>();
			Map<String, Object> queueCheck = new HashMap<String, Object>();
			queueCheck.put("techDetail", "Queue has at least 5 tracks");
			queueCheck.put("ok", status.getPlaylist().getLength() >= 5);
			checks.put("queue", queueCheck);
			Map<String, Object> emptyQueueCheck = new HashMap<String, Object>();
			emptyQueueCheck.put("techDetail", "Queue has any tracks");
			emptyQueueCheck.put("ok", status.getPlaylist().getLength() > 0);
			checks.put("empty-queue", emptyQueueCheck);
			Map<String, Object> queueMetric = new HashMap<String, Object>();
			queueMetric.put("techDetail", "Number of tracks in queue");
			queueMetric.put("value", status.getPlaylist().getLength());
			metrics.put("queue-length", queueMetric);
			Map<String, String> ci = new HashMap<String, String>();
			ci.put("circle", "gh/lucas42/lucos_media_manager");
			output.put("system", "lucos_media_manager");
			output.put("checks", checks);
			output.put("metrics", metrics);
			output.put("ci", ci);
			sendHeaders(200, "OK", "application/json");
			osw.write(gson.toJson(output));
		} else if(path.equals("/robots.txt")) {
			sendHeaders(200, "OK", "text/plain");
			osw.write("User-agent: *\nDisallow: /\n");
		} else if(path.equals("/trackUpdated") && post) {
			try {
				LoganneTrackEvent event = gson.fromJson(data, LoganneTrackEvent.class);
				status.getPlaylist().updateTracks(event.track.getMetadata("trackid"), event.track);
				sendHeaders(204, "No Content", "text/plain");
			} catch (JsonSyntaxException exception) {
				sendHeaders(400, "Bad Request", "text/plain");
				osw.write("JSON Syntax Error"+CRLF);
				osw.write(exception.getMessage()+CRLF);
			}
		} else {
			final Set<String> PLAYER_REDIRECTS = Set.of(
			    "/",
			    "/player",
			    "/controller",
			    "/mobile",
			    "/controller-icon",
			    "/player-icon",
			    "/basic/next",
			    "/basic/playpause"
			);
			if (PLAYER_REDIRECTS.contains(path)) {
				System.err.println("WARNING: Using deprected "+path+", redirecting to seinn.l42.eu");
				redirect("https://seinn.l42.eu/");
			} else {
				System.err.println("WARNING: File Not found: ".concat(path));
				sendHeaders(404, "Not Found", "text/plain");
				osw.write("File Not Found" + CRLF);
			}
		}
		
		// Close streams and socket.
		osw.close();
		br.close();
		socket.close();

	}

	private float getFloat(String param, boolean head) throws IOException {
		try{
			float value = Float.parseFloat(get.get(param));
			return value;
		} catch (NumberFormatException e) {
			sendHeaders(400, "Not Changed", "application/json");
			if (!head) osw.write(param + " must be a number");
		} catch (NullPointerException e) {
			if (!head) sendHeaders(400, "Not Changed", "application/json");
		}
		return -1;
	}
	private void sendHeaders(int status, String statusstring, Map<String, String> extraheaders) throws IOException {
		os.writeBytes("HTTP/1.1 "+ status +" "+ statusstring + CRLF);
		os.writeBytes("Access-Control-Allow-Origin: *" + CRLF);
		os.writeBytes("Server: lucos" + CRLF);
		Iterator iter = extraheaders.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry header = (Map.Entry)iter.next();
			os.writeBytes(header.getKey()+": "+header.getValue() + CRLF);
		}
		os.writeBytes(CRLF);
	}
	private void sendHeaders(int status, String statusstring, String contentType) throws IOException {
		HashMap<String, String> headers =  new HashMap<String, String>();
		headers.put("Content-type", contentType+ "; charset=utf-8");
		sendHeaders(status, statusstring, headers);
	}
	private void redirect(String url) throws IOException {
		HashMap<String, String> headers =  new HashMap<String, String>();
		headers.put("Location", url);
		sendHeaders(302, "Redirect", headers);
	}
}
