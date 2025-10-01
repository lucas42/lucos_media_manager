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
		if (System.getenv("CLIENT_KEYS") == null) {
			System.err.println("FATAL: No CLIENT_KEYS environment variable specified");
			System.exit(1);
		}
		if (System.getenv("STATE_DIR") == null) {
			System.err.println("FATAL: No STATE_DIR environment variable specified");
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

		HttpRequest.setClientKeys(System.getenv("CLIENT_KEYS"));

		Loganne loganne = new Loganne("lucos_media_manager", System.getenv("LOGANNE_ENDPOINT"));
		MediaApi mediaApi = new MediaApi();

		FileSystemSync fsSync = new FileSystemSync(System.getenv("STATE_DIR"));
		Status status = fsSync.readStatus(loganne, mediaApi);

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
