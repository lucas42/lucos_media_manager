import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Handles webhook requests which come from loganne
 */
class WebhookController extends Controller {
	public WebhookController(Status status, HttpRequest request) {
		super(status, request);
	}
	public void processRequest() throws IOException {
		String hookname = request.getPath().replaceFirst("^/webhooks/", "");
		if (request.getMethod() == Method.POST) {
			Gson gson = CustomGson.get(status);
			try {
				if (hookname.equals("trackUpdated")) {
					LoganneTrackEvent event = gson.fromJson(request.getData(), LoganneTrackEvent.class);
					status.getPlaylist().updateTracks(event.track.getMetadata("trackid"), event.track);
					request.sendHeaders(204, "No Content");
					request.close();
				} else if(hookname.equals("trackDeleted")) {
					LoganneTrackEvent event = gson.fromJson(request.getData(), LoganneTrackEvent.class);
					status.getPlaylist().deleteTrack(event.track);
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
			}
		} else {
			request.notAllowed(Arrays.asList(Method.POST));
		}
	}
}