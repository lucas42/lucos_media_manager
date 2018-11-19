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
final class HttpRequest implements Runnable {
	final static String CRLF = "\r\n";
	Socket socket;
	String fullHostname;
	String hostname;
	DataOutputStream os;
	OutputStreamWriter osw;
	Map<String, String> header = new HashMap<String, String>();
	Map<String, String> get = new HashMap<String, String>();
	static Gson gson = new Gson();
	
	// Constructor
	public HttpRequest(Socket socket) throws Exception {
		this.socket = socket;
		fullHostname = socket.getInetAddress().getHostName();
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

	}
	private void processRequest() throws Exception {
	
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
				update_success = Manager.update(update_curtrack, update_currentTime, update_currentTimeSet);
			} catch (Exception e) {
				System.err.println("ERROR: Unexpected error updating manager");
				e.printStackTrace();
			}
		}
		if (path.equals("/poll")) {
			long startTime = System.nanoTime();
			int hashcode;
			try {
				hashcode = Integer.parseInt(get.get("hashcode"));
			} catch (NumberFormatException e) {
				hashcode = 0;
			}
			while (true) {
				if (Manager.hasChanged(hashcode)) {
					sendHeaders(200, "Long Poll", "application/json");
					if (!head) osw.write(gson.toJson(Manager.getStatus()));   
					break;
				}
				if ((System.nanoTime() - startTime) > (1000000000L * 30)) {
					sendHeaders(200, "Long Poll", "application/json");
					if (!head) osw.write("{ }");
					break;
				}
				
				Thread.sleep(1);
			}
		} else if (path.equals("/poll/playlist")) {
			long startTime = System.nanoTime();
			int hashcode;
			try {
				hashcode = Integer.parseInt(get.get("hashcode"));
			} catch (NumberFormatException e) {
				hashcode = 0;
			}
			while (true) {
				if (Manager.playlistHasChanged(hashcode)) {
					sendHeaders(200, "Long Poll", "application/json");
					if (!head) osw.write(gson.toJson(Manager.getPlaylist()));   
					break;
				}
				if ((System.nanoTime() - startTime) > (1000000000L * 30)) {
					sendHeaders(200, "Long Poll", "application/json");
					if (!head) osw.write("{ }");
					break;
				}
				
				Thread.sleep(1);
			}
		// Returns the number of tracks currently in the playlist (can be used for monitoring etc)
		} else if (path.equals("/poll/playlist/length")) {
			sendHeaders(200, "OK", "text/plain");
			if (!head) osw.write(gson.toJson(Manager.getPlaylistLength()));

		// Includes all tracks, including current playing and queued in playlist
		} else if (path.equals("/poll/summary")) {
			long startTime = System.nanoTime();
			int hashcode;
			try {
				hashcode = Integer.parseInt(get.get("hashcode"));
			} catch (NumberFormatException e) {
				hashcode = 0;
			}
			while (true) {
				if (Manager.fullSummaryHasChanged(hashcode)) {
					sendHeaders(200, "Long Poll", "application/json");
					if (!head) osw.write(gson.toJson(Manager.getFullSummary()));   
					break;
				}
				if ((System.nanoTime() - startTime) > (1000000000L * 30)) {
					sendHeaders(200, "Long Poll", "application/json");
					if (!head) osw.write("{ }");
					break;
				}
				
				Thread.sleep(1);
			}
		} else if (path.equals("/control") && post) {
			Manager.update(get);
			sendHeaders(204, "Changed", "application/json");
		} else if (path.equals("/next") && post) {
			if (get.get("now") == null || Manager.isCurrentURL(get.get("now"))) {
				Manager.next();
				sendHeaders(204, "Changed", "application/json");
			} else {
				sendHeaders(204, "Wrong current track", "application/json");
			}
		} else if (path.equals("/play") && post) {
			Manager.setPlaying(true);
			sendHeaders(204, "Changed", "application/json");
		} else if (path.equals("/pause") && post) {
			Manager.setPlaying(false);
			sendHeaders(204, "Changed", "application/json");
		} else if (path.equals("/playpause")) {
			System.err.println("WARNING: Using deprected /playpause");
			
			// Try to use /play and /pause instead.
			Manager.TogglePlayPause();
			sendHeaders(204, "Changed", "application/json");
			
		// Basic paths for user agents without JS
		} else if (path.equals("/basic/next") && post) {
			Manager.next();
			
			// Use full url for lynx optimisation
			redirect("https://".concat(header.get("Host").trim().concat("/")));
		} else if (path.equals("/basic/playpause") && post) {
			Manager.TogglePlayPause();
			
			// Use full url for lynx optimisation
			redirect("https://".concat(header.get("Host").trim().concat("/")));
		} else if (path.equals("/volume") && post) {
			float volume = getFloat("volume", head);
			if (volume != -1) {
				Manager.setVolume(volume);
				sendHeaders(204, "Changed", "application/json");
			}
		} else if (path.equals("/done") && post) {
			Track oldtrack = new Track(get.get("track"));
			String status = get.get("status");
			Manager.finished(oldtrack, status);
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
				Manager.queue(newTrack, -1);
			} else if (pos.equals("next")) {
				Manager.queue(newTrack, 0);
			} else {
				Manager.queue(newTrack);
			}
			sendHeaders(204, "Queued", "application/json");
		} else if (path.equals("/insert")) {
			// deprecated, use /queue?pos=next instead
			redirect(fullpath.replaceFirst("/insert\\?", "/queue?pos=next&"));
		} else if (path.equals("/openexturl") && post) {
			if (Manager.openExtUrl()) sendHeaders(204, "Changed", "application/json");
			else sendHeaders(400, "Can't find external url", "application/json");
		} else if (path.equals("/openediturl") && post) {
			if (Manager.openEditUrl()) sendHeaders(204, "Changed", "application/json");
			else sendHeaders(400, "Can't find edit url", "application/json");
		}  else if (path.equals("/time")) {
			redirect("am.l42.eu");
		} else {
			String fileName;
			
			// TODO: remove hardcoded absolute paths
			if (path.equals("/player")) fileName = "/web/lucos/lucos_media_player/";
			else if (path.equals("/")) fileName = "/web/lucos/lucos_media_controller/index.xhtml";
			else if (path.equals("/controller")) fileName = "/web/lucos/lucos_media_controller/";
			else if (path.equals("/mobile")) fileName = "/web/lucos/lucos_media_controller/mobile.html";
			else if (path.equals("/controller-icon")) fileName = "/web/lucos/lucos_media_controller/icon.png";
			else if (path.equals("/player-icon")) fileName = "/web/lucos/lucos_media_player/icon.png";
			else fileName = "./data" + path;
			fileName.replaceAll("/\\.\\./","");
			if (fileName.charAt(fileName.length()-1) == '/') fileName += "index.html";
			
			// Open the requested file.
			FileInputStream fis = null;
			boolean fileExists = true;
			String statusLine = null;
			try {
				fis = new FileInputStream(fileName);
				 statusLine = "HTTP/1.1 200 OK";
			} catch (FileNotFoundException e) {
				System.err.println("WARNING: File Not found: ".concat(fileName));
				fileName = "./data/404.html";
				statusLine = "HTTP/1.1 404 File Not Found";
				try {
					fis = new FileInputStream(fileName);
				} catch (FileNotFoundException e2) {
					fileExists = false;
				}
			}



			// Construct the response message.
			String contentTypeLine = null;
			String entityBody = null;
			if (fileExists) {
				contentTypeLine = "Content-Type: " +
					contentType( fileName ) + "; charset=UTF-8";
			}
			// Send the status line.
			os.writeBytes(statusLine + CRLF);
			// Send the content type line.
			if (contentTypeLine != null) os.writeBytes(contentTypeLine + CRLF);
			// Send a blank line to indicate the end of the header lines.
			os.writeBytes(CRLF);

			// Send the entity body.
			if (fileExists) {
				 sendBytes(fis);
				 fis.close();
			} else {
				 os.writeBytes("error: 404 file not found");
			}
		}
		
		// Close streams and socket.
		osw.close();
		br.close();
		socket.close();

	}



	private void sendBytes(FileInputStream fis) throws Exception {
	   // Construct a 1K buffer to hold bytes on their way to the socket.
	   byte[] buffer = new byte[1024];
	   int bytes = 0;
	   
	   // Copy requested file into the socket's output stream.
	   while((bytes = fis.read(buffer)) != -1 ) {
		  os.write(buffer, 0, bytes);
	   }
	}

	private static String contentType(String fileName) {
		if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
			return "text/html";
		}
		if(fileName.endsWith(".xhtml")) {
			return "application/xhtml+xml";
		}
		if(fileName.endsWith(".png")) {
			return "image/png";
		}
		if(fileName.endsWith(".gif")) {
			return "image/gif";
		}
		if(fileName.endsWith(".jpg")) {
			return "image/jpeg";
		}
		if(fileName.endsWith(".css")) {
			return "text/css";
		}
		if(fileName.endsWith(".js")) {
			return "text/javascript";
		}
		if(fileName.endsWith(".mp3")) {
			return "audio/mpeg";
		}
		if(fileName.endsWith(".txt")) {
			return "text/plain";
		}
		if(fileName.endsWith("manifest")) {
			return "text/cache-manifest";
		}
		return "application/octet-stream";
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
