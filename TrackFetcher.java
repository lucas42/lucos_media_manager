import java.io.* ;
import java.net.* ;
import java.util.* ; 
import java.lang.* ; 
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
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
		String playlisturl = "http://"+Manager.getHost("mediaselector")+"/playlist";	
		getXML(playlisturl);
	}
	private BufferedReader getHTTP(String host, String path, String auth) throws Exception {
		Socket socket = new Socket(host, 80);
		
		// Get a reference to the socket's input and output streams.
		InputStream is = new DataInputStream(socket.getInputStream());
		DataOutputStream os = new DataOutputStream(socket.getOutputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF8"));
		
		os.writeBytes("GET "+path+" HTTP/1.1" + CRLF);
		os.writeBytes("Host: " + host + CRLF);
		os.writeBytes("Authorization: "+auth + CRLF);
		os.writeBytes(CRLF);
		String line;
		
		// Ignore headers
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.length() < 1) break;
		}
		return br;
	}
	private BufferedReader getLocal(String path) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(path));
		return br;
	}
	private void getXML(String listurl) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(listurl);
		NodeList tracks = document.getElementsByTagName("track");
		for (int ii=0; ii < tracks.getLength(); ii++) {
			HashMap<String, String> data = new HashMap<String, String>();
			NodeList children = tracks.item(ii).getChildNodes();
			String url = null;
			for (int jj=0; jj < children.getLength(); jj++) {
				Node child = children.item(jj);
				if (child.getNodeType() != Node.ELEMENT_NODE ) continue;
				String name = child.getNodeName();
				String value = getXMLText(child);
				if (name == "url") url = value;
				else data.put(name, value);
			}
			if (url == null) {
				System.out.println("Track missing url");
			}
			Track track = new Track(url, data);
			Manager.queue(track);
		}
	}
	private String getXMLText(Node node) {
		if (node.getNodeType() == Node.TEXT_NODE ) return node.getNodeValue();
		if (node.getNodeType() != Node.ELEMENT_NODE ) return "";
		String output = "";
		NodeList children = node.getChildNodes();
		for (int ii=0; ii < children.getLength(); ii++) {
			output += getXMLText(children.item(ii));
		}
		return output;
	}
}
