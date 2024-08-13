import java.io.IOException;
/**
 * Handles requests for robots.txt
 */
class RobotsController extends Controller {
	public RobotsController(Status status, HttpRequest request) {
		super(status, request);
	}
	protected void processRequest() throws IOException {
		request.sendHeaders(200, "OK", "text/plain");
		request.writeBody("User-agent: *");
		request.writeBody("Disallow: /");
		request.close();
	}
}