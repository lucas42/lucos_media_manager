import java.io.IOException;
import java.util.Set;

/**
 * Handles requests which can't be handled by any other controller
 */
class FallbackController extends Controller {
	public FallbackController(Status status, HttpRequest request) {
		super(status, request);
	}
	protected void processRequest() throws IOException {

		// V1 included media player and API on same domain
		// For urls used by media player, redirect to new media player domain
		final Set<String> PLAYER_REDIRECTS = Set.of(
			"/",
			"/player",
			"/controller",
			"/mobile",
			"/controller-icon",
			"/player-icon",
			"/basic/next",
			"/basic/playpause"
		);
		if (PLAYER_REDIRECTS.contains(request.getPath())) {
			System.err.println("WARNING: Using deprected "+request.getPath()+", redirecting to seinn.l42.eu");
			request.redirect("https://seinn.l42.eu/");
			request.close();
			return;
		}

		// V1 & V2 didn't have any versioning on their paths
		// So list all API paths used by them and return a gone response
		final Set<String> ENDPOINT_GONE = Set.of(
			"/poll/summary",
			"/play",
			"/pause",
			"/mobile",
			"/volume",
			"/next",
			"/done",
			"/error",
			"/update",
			"/collection",
			"/devices",
			"/devices/current",
			"/trackUpdated",
			"/trackDeleted",
			"/collectionsChanged"
		);
		if (ENDPOINT_GONE.contains(request.getPath())) {
			System.err.println("WARNING: Endpoint Gone: ".concat(request.getPath()));
			request.sendHeaders(410, "Gone", "text/plain");
			request.writeBody("Endpoint no longer supported");
			request.close();
			return;
		}

		// For anything thing else, return a not found response
		System.err.println("WARNING: File Not found: ".concat(request.getPath()));
		request.sendHeaders(404, "Not Found", "text/plain");
		request.writeBody("File Not Found");
		request.close();
	}
}