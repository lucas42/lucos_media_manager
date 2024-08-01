import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;

/**
 * Handles requests to the /_info endpoint
 */
class InfoController implements Controller {
	private Status status;
	private HttpRequest request;

	public InfoController(Status status, HttpRequest request) {
		this.status = status;
		this.request = request;
	}
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			System.err.println("ERROR: Unknown Error (InfoController, host:"+request.getHostName()+"):");
			e.printStackTrace();
		}
	}
	public void processRequest() throws IOException {
		Gson gson = CustomGson.get(status);
		Map<String, Object> output = new HashMap<String, Object>();
		Map<String, Map<String, Object>> checks = new HashMap<String, Map<String, Object>>();
		Map<String, Map<String, Object>> metrics = new HashMap<String, Map<String, Object>>();
		Map<String, Object> queueCheck = new HashMap<String, Object>();
		queueCheck.put("techDetail", "Queue has at least 5 tracks");
		queueCheck.put("ok", status.getPlaylist().getLength() >= 5);
		checks.put("queue", queueCheck);
		Map<String, Object> emptyQueueCheck = new HashMap<String, Object>();
		emptyQueueCheck.put("techDetail", "Queue has any tracks");
		emptyQueueCheck.put("ok", status.getPlaylist().getLength() > 0);
		checks.put("empty-queue", emptyQueueCheck);
		Map<String, Object> queueMetric = new HashMap<String, Object>();
		queueMetric.put("techDetail", "Number of tracks in queue");
		queueMetric.put("value", status.getPlaylist().getLength());
		metrics.put("queue-length", queueMetric);
		Map<String, String> ci = new HashMap<String, String>();
		ci.put("circle", "gh/lucas42/lucos_media_manager");
		output.put("system", "lucos_media_manager");
		output.put("checks", checks);
		output.put("metrics", metrics);
		output.put("ci", ci);
		request.sendHeaders(200, "OK", "application/json");
		request.writeBody(gson.toJson(output));
		request.close();
	}
}