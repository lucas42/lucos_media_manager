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
		Map<String, String> header = new HashMap<String, String>();
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
		} else {
			method = null;
		}
		path = tokens.nextToken().trim();
		
		data = "";
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
		this.sendHeaders(405, "Method Not Allowed", Map.of("Allow", allow));
		this.close();
	}
}
