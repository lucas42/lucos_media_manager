import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.net.URI;

/**
 * Handles webhook requests which come from loganne
 */
class WebhookController extends Controller {
	public WebhookController(Status status, HttpRequest request) {
		super(status, request);
	}
	protected void processRequest() throws IOException {
		if (!request.isAuthorised()) {
			request.sendHeaders(401, "Unauthorized", Map.of(
				"Content-Type", "text/plain",
				"WWW-Authenticate", "Bearer"
			));
			request.writeBody("Invalid API Key");
			request.close();
			return;
		}
		String hookname = request.getPath().replaceFirst("^/webhooks/", "");
		if (request.getMethod() == Method.POST) {
			Gson gson = new Gson();
			try {
				if (hookname.equals("trackUpdated")) {
					LoganneEvent event = gson.fromJson(request.getData(), LoganneEvent.class);
					if (event == null || event.url == null) throw new JsonSyntaxException("Missing url field in event");
					String trackPath = URI.create(event.url).getPath();
					Track fetchedTrack = status.getMediaApi().fetchTrack(trackPath);
					if (fetchedTrack == null) throw new IOException("Media API returned null for track at " + event.url);
					status.getPlaylist().updateTracks(fetchedTrack.getMetadata("trackid"), fetchedTrack);
					request.sendHeaders(204, "No Content");
					request.close();
				} else if(hookname.equals("trackDeleted")) {
					LoganneEvent event = gson.fromJson(request.getData(), LoganneEvent.class);
					if (event == null || event.url == null) throw new JsonSyntaxException("Missing url field in event");
					String trackId = URI.create(event.url).getPath().replaceFirst("^.*/", "");
					status.getPlaylist().deleteTrackById(trackId);
					request.sendHeaders(204, "No Content");
					request.close();

				// The list of collections isn't too big, so if anything changes about any collection, just refresh the whole list
				} else if(List.of("collectionCreated","collectionUpdated","collectionDeleted").contains(hookname)) {
					if (status.getCollectionList().refreshList()) {
						request.sendHeaders(204, "No Content");
					} else {
						request.sendHeaders(500, "Internal Server Error", "text/plain");
						request.writeBody("Failed to fetch collections from media API");
					}
					request.close();
				} else {
					System.err.println("WARNING: Webhook Not found: "+hookname);
					request.sendHeaders(404, "Not Found", "text/plain");
					request.writeBody("Can't find webhook \""+hookname+"\"");
					request.close();
				}
			} catch (JsonSyntaxException exception) {
				request.sendHeaders(400, "Bad Request");
				request.writeBody("JSON Syntax Error");
				request.writeBody(exception.getMessage());
				request.close();
			} catch (IOException exception) {
				System.err.println("ERROR: Failed to fetch track from media API: " + exception.getMessage());
				request.sendHeaders(500, "Internal Server Error", "text/plain");
				request.writeBody("Failed to fetch track from media API");
				request.close();
			}
		} else {
			request.notAllowed(Arrays.asList(Method.POST));
		}
	}
}