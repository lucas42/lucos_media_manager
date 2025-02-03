import java.io.IOException;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Collection;
import java.util.stream.Collectors;
class HttpRequest {
	final static String CRLF = "\r\n";
	private Socket socket;
	private DataOutputStream os;
	private OutputStreamWriter osw;
	private BufferedReader br;
	private Map<String, String> getParameters = new HashMap<String, String>();
	private String path;
	private String data;
	private Method method;
	private String authorizationHeader;
	private int responseCode;
	static private Collection<String> validApiKeys = new HashSet<String>();
	
	// Constructor
	public HttpRequest(Socket socket) {
		this.socket = socket;
	}

	public String getHostName() {
		return socket.getInetAddress().getHostName();
	}

	public void readFromSocket() throws IOException {
	
		// Get a reference to the socket's input and output streams.
		InputStream is = new DataInputStream(socket.getInputStream());
		os = new DataOutputStream(socket.getOutputStream());
		osw = new OutputStreamWriter(os, "UTF8");
		br = new BufferedReader(new InputStreamReader(is, "UTF8"));

		String requestLine = br.readLine();

		if (requestLine == null) {
			throw new IOException("Incoming HTTP Request interrupted before receiving headers.  Aborting response.");
		}

		// Get the header lines.
		String headerLine = null;
		Map<String, String> headers = new HashMap<String, String>();
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
			headers.put(headerLine.toLowerCase(), field.trim());
		}
		
		// Extract the filename from the request line.
		StringTokenizer tokens = new StringTokenizer(requestLine);
		String methodString = tokens.nextToken();
		if (methodString.equalsIgnoreCase("HEAD")) {
			method = Method.HEAD;
		} else if (methodString.equalsIgnoreCase("POST")) {
			method = Method.POST;
		} else if (methodString.equalsIgnoreCase("PUT")) {
			method = Method.PUT;
		} else if (methodString.equalsIgnoreCase("GET")) {
			method = Method.GET;
		} else if (methodString.equalsIgnoreCase("DELETE")) {
			method = Method.DELETE;
		} else if (methodString.equalsIgnoreCase("OPTIONS")) {
			method = Method.OPTIONS;
		} else {
			System.err.println("WARNING: Unrecognised HTTP Method "+methodString);
			method = Method.UNKNOWN;
		}
		path = tokens.nextToken().trim();
		
		data = "";
		if (headers.containsKey("content-length")) {
			try {
				int length = Integer.parseInt(headers.get("content-length"));
				char[] cbuf = new char[length];
				br.read(cbuf, 0, length);
				data = new String(cbuf);
			} catch (Exception e) {
				System.err.println("WARNING: Can't get HTTP data from request:");
				e.printStackTrace(System.err);
			}
		}
		authorizationHeader = headers.get("authorization");
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
				getParameters.put(key, field);
			}
			path = path.substring(0, ii);
		}
	}


	public void close() throws IOException {

		// Close socket and stream writter.
		osw.close();
		br.close();
		socket.close();
	}

	public String getParam(String key) {
		return getParameters.get(key);
	}
	public String getParam(String key, String defaultValue) {
		return getParameters.getOrDefault(key, defaultValue);
	}
	public String removeParam(String key) {
		return getParameters.remove(key);
	}
	public String getPath() {
		return path;
	}
	public String getData() {
		return data.trim();
	}
	public Method getMethod() {
		return method;
	}
	// Checks whether the request has an API key matching one listed in the CLIENT_KEYS environment variable
	public boolean isAuthorised() {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Key ")) {
			return false;
		}
		String apiKey = authorizationHeader.replaceFirst("Key ", "");
		return validApiKeys.contains(apiKey);
	}

	public void sendHeaders(int status, String statusstring, Map<String, String> extraheaders) throws IOException {
		os.writeBytes("HTTP/1.1 "+ status +" "+ statusstring + CRLF);
		os.writeBytes("Access-Control-Allow-Origin: *" + CRLF);
		os.writeBytes("Server: lucos" + CRLF);
		Iterator iter = extraheaders.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry header = (Map.Entry)iter.next();
			os.writeBytes(header.getKey()+": "+header.getValue() + CRLF);
		}
		os.writeBytes(CRLF);
		responseCode = status;
	}
	public void sendHeaders(int status, String statusstring, String contentType) throws IOException {
		HashMap<String, String> headers =  new HashMap<String, String>();
		headers.put("Content-Type", contentType+ "; charset=utf-8");
		sendHeaders(status, statusstring, headers);
	}
	public void sendHeaders(int status, String statusstring) throws IOException {
		sendHeaders(status, statusstring, new HashMap<String, String>());
	}
	public void redirect(String url) throws IOException {
		HashMap<String, String> headers =  new HashMap<String, String>();
		headers.put("Location", url);
		sendHeaders(302, "Redirect", headers);
	}
	public void writeBody(String content) throws IOException {
		if (method == Method.HEAD) return; // HEAD Requests shouldn't return a body
		osw.write(content + CRLF);
	}

	// Convenience method for returning a 405 "Method Not Allowed" response including the "Allow" header
	public void notAllowed(Collection<Method> allowedMethods) throws IOException {
		String allow = allowedMethods.stream().map( method -> method.name() ).collect(Collectors.joining (", "));
		if (this.getMethod().equals(Method.OPTIONS)) { // Special case for OPTIONS method - treat as CORS-preflight request
			this.sendHeaders(204, "No Content", Map.of(
				"Access-Control-Allow-Methods", allow,
				"Access-Control-Allow-Headers", "Authorization"
			));
		} else {
			this.sendHeaders(405, "Method Not Allowed", Map.of("Allow", allow));
		}
		this.close();
	}

	/**
	 * Checks whether this request _could_ have altered state
	 * based on HTTP method and resonse code
	 **/
	public boolean alteredState() {

		// If the HTTP Method is Safe, then state shouldn't have changed
		if (Set.of(Method.HEAD, Method.GET, Method.OPTIONS, Method.UNKNOWN).contains(this.getMethod())) return false;

		// If the response is a redirect or error, then state shouldn't have changed
		if (responseCode >= 300) return false;

		// Otherwise, the state could have changed
		return true;
	}

	static public void setClientKeys(String rawClientKeys) {
		validApiKeys = Arrays.asList(rawClientKeys.split(";")).stream().map(client -> client.replaceFirst("^.*=","")).collect(Collectors.toList());
	}
}
