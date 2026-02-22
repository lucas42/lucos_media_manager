import java.io.IOException;

// Controller for deciding which controller to use for a given request
class FrontController extends Controller {

	// Constructor
	public FrontController(Status status, HttpRequest request) {
		super(status, request);
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
		if (request.getPath().equals("/robots.txt")) {
			return new RobotsController(status, request);
		}
		if (request.getPath().startsWith("/webhooks/")) {
			return new WebhookController(status, request);
		}
		if (request.getPath().equals("/v3/poll")) {
			return new LongPollControllerV3(status, request, 30, 3);
		}
		if (request.getPath().startsWith("/v3/") || request.getPath().equals("/v3")) {
			return new ControllerV3(status, request);
		}
		return new FallbackController(status, request);
	}
}