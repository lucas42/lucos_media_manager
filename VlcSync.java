import java.io.* ;
import java.net.* ;
import java.util.* ; 
import java.lang.* ; 
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
final class VlcSync implements Runnable {
	public static boolean pausedForVlc = false;
	
	// Implement the run() method of the Runnable interface.
	public void run() {
		while(true) {
			try {
				try {
					processRequest();
					Thread.sleep(2000);
				} catch (IOException e) {
					Thread.sleep(30000);
				} catch (Exception e) {
					System.err.println("VlcSync Error:");
					e.printStackTrace(System.err);
				}
			} catch (InterruptedException e) {
				System.err.println("Thread sleep Error:");
				e.printStackTrace(System.err);
			}
		}
	}
	private void processRequest() throws Exception {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(Manager.getSetting("vlcweb"));
		String state = getXMLText(document.getElementsByTagName("state").item(0));
		if (state.equals("playing") && Manager.getPlaying()) {
			Manager.setPlaying(false);
			pausedForVlc = true;
		}
		if (pausedForVlc && !state.equals("playing")) {
			
			// Wait a second and make sure that vlc isn't just between tracks
			Thread.sleep(1000);
			if (pausedForVlc && !state.equals("playing")) {
				Manager.setPlaying(true);
			}
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
