import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import com.google.gson.Gson;

/**
 * Handles requests to the /_info endpoint
 */
class InfoController extends Controller {
	public InfoController(Status status, HttpRequest request) {
		super(status, request);
	}
	protected void processRequest() throws IOException {
		Gson gson = CustomGson.get(status);
		Map<String, Object> output = new LinkedHashMap<String, Object>();
		Map<String, Map<String, Object>> checks = new HashMap<String, Map<String, Object>>();
		Map<String, Map<String, Object>> metrics = new HashMap<String, Map<String, Object>>();
		Playlist playlist = status.getPlaylist();
		boolean repopulating = playlist.isFetcherRunning() && !playlist.getFetcher().hasFailedAttempt();
		Map<String, Object> queueCheck = new HashMap<String, Object>();
		queueCheck.put("techDetail", "Queue has at least 5 tracks, or is being actively repopulated (not retrying after a failure)");
		queueCheck.put("ok", playlist.getLength() >= 5 || repopulating);
		queueCheck.put("failThreshold", 3);
		checks.put("queue", queueCheck);
		Map<String, Object> emptyQueueCheck = new HashMap<String, Object>();
		emptyQueueCheck.put("techDetail", "Queue has any tracks, or is being actively repopulated (not retrying after a failure)");
		emptyQueueCheck.put("ok", playlist.getLength() > 0 || repopulating);
		emptyQueueCheck.put("failThreshold", 2);
		checks.put("empty-queue", emptyQueueCheck);
		Map<String, Object> queueMetric = new HashMap<String, Object>();
		queueMetric.put("techDetail", "Number of tracks in queue");
		queueMetric.put("value", status.getPlaylist().getLength());
		metrics.put("queue-length", queueMetric);
		Map<String, String> ci = new HashMap<String, String>();
		ci.put("circle", "gh/lucas42/lucos_media_manager");
		output.put("system", "lucos_media_manager");
		output.put("title", "Media Manager");
		output.put("checks", checks);
		output.put("metrics", metrics);
		output.put("ci", ci);
		request.sendHeaders(200, "OK", "application/json");
		request.writeBody(gson.toJson(output));
		request.close();
	}
}