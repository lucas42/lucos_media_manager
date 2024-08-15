import java.io.* ;
import java.net.* ;
import java.util.* ;
import java.math.BigInteger;
public final class Manager {
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
		Loganne loganne = new Loganne("lucos_media_manager", "https://loganne.l42.eu");


		Playlist playlist = new Playlist(new RandomFetcher(), loganne);

		// TODO: Keep state of device list between restarts
		DeviceList deviceList = new DeviceList(loganne);

		CollectionList collectionList = new CollectionList();

		MediaApi mediaApi= new MediaApi();
		
		Status status = new Status(playlist, deviceList, collectionList, mediaApi);

		// Establish the listen socket.
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("INFO: outgoing data server ready on port "+port);
    
		// Process HTTP service requests in an infinite loop.
		while (true) {
			// Listen for a TCP connection request.

			Socket clientSocket = serverSocket.accept();
			// Construct an object to process the HTTP request message.
			FrontController controller = new FrontController(status, new HttpRequest(clientSocket));
			// Create a new thread to process the request.
			Thread thread = new Thread(controller);
			// Start the thread.
			thread.start();

		}
	
	}
}
