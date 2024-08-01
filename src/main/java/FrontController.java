import java.io.IOException;
import java.net.Socket;
// Controller for deciding which controller to use for a given request
class FrontController extends Controller {

	// Constructor
	public FrontController(Status status, Socket socket) {
		super(status, new HttpRequest(socket));
	}

	protected void processRequest() throws IOException {
		request.readFromSocket();
		Controller controller = chooseController();
		controller.run();
	}

	// Based on the request, decide which controller to use
	private Controller chooseController() {
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