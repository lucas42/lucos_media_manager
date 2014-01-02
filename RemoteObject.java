import java.io.* ;
import java.net.* ;
import java.util.* ; 
class RemoteObject {
	private HttpURLConnection connection;
	public RemoteObject (String url) throws IOException {
		try{
			URL dataurl = new URL(url);
			connection = (HttpURLConnection) dataurl.openConnection();
		} catch (MalformedURLException e) {
			System.err.println("Invalid URL: "+url);
			System.err.println(e);
		}
	}
	public int getResponseCode() {
		try {
			return connection.getResponseCode();
		} catch (IOException e) {
			return 0;
		}
	}
	public String getBody() throws IOException {
		InputStream is;
		if (getResponseCode() < 300) {
        	is = connection.getInputStream();
        } else {
        	is = connection.getErrorStream();
        }
        BufferedReader datain = new BufferedReader(new InputStreamReader(is));
        String data = "";
        String datastr;
        while ((datastr = datain.readLine()) != null) {
            data += datastr;
        }
        datain.close();
        return data;
	}
}