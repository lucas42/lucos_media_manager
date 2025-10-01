import java.net.*;
import java.io.*;
import java.util.*;
import com.google.gson.Gson;
class Loganne {
	private String source;
	private URL url;
	static Gson gson = new Gson();
	public Loganne(String sourceService, String endpoint) throws MalformedURLException {
		source = sourceService;
		url = new URL(endpoint);
	}

	/**
	 * Asynchronously sends an event to the loganne service
	 */
	public void post(String type, String humanReadable) {
		Thread thread = new Thread(){
			public void run(){
				try {
					rawPost(type, humanReadable);
				} catch (Exception e) {
					System.err.println("Can't post to Loganne");
					e.printStackTrace();
				}
			}
		};

		thread.start();
	} 
	private void rawPost(String type, String humanReadable) throws IOException {
		Map<String, Object> postData = new HashMap<String, Object>();
		postData.put("source", source);
		postData.put("type", type);
		postData.put("humanReadable", humanReadable);
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		con.setDoOutput(true);

		OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), "UTF8");
		osw.write(gson.toJson(postData));
		osw.close();
		con.disconnect();

		try {
			// Don't care about the contents of the response, just that it returns a 2xx status code
			// getInputStream throws an exception for error status codes
			con.getInputStream();
		} catch (IOException e) {
			InputStream error = con.getErrorStream();
			if (error == null) throw e;
			String err = "";
			for (int nextByte = 0; nextByte > -1; nextByte = error.read()) {
				err += (char) nextByte;
			}
			throw new IOException(con.getResponseMessage() + ": " + err);
		}
	}
}