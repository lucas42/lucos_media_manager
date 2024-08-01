import java.io.IOException;
import java.net.Socket;
// Controller for deciding which controller to use for a given request
class FrontController implements Controller {
	private Status status;
	private HttpRequest request;

	// Constructor
	public FrontController(Status status, Socket socket) {
		this.status = status;
		request = new HttpRequest(socket);
	}

	// Implement the run() method of the Runnable interface.
	public void run() {
		try {
			Controller controller = chooseController();
			controller.run();
		} catch (IOException e) {
			System.out.println("WARNING: Problem with socket on incoming HTTP Request. "+e.getMessage());
		}
	}

	// Convenience method for unit tests, to avoid error handling and waiting done by the run() method
	public void processRequest() throws Exception {
		Controller controller = chooseController();
		controller.processRequest();
	}

	// Based on the request, decide which controller to use
	private Controller chooseController() throws IOException {
		request.readFromSocket();
		if (request.getPath().equals("/_info")) {
			return new InfoController(status, request);
		}
		if (request.getPath().startsWith("/webhooks/")) {
			return new WebhookController(status, request);
		}
		if (request.getPath().startsWith("/v3/") || request.getPath().equals("/v3")) {
			return new ControllerV3(status, request);
		}
		return new ControllerV2(status, request);
	}
}