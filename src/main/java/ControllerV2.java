import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.math.BigInteger;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
// Legacy Controller which handles all v2 endpoints
class ControllerV2 extends Controller {
	private boolean update_success;
	private Device device;

	// Constructor
	public ControllerV2(Status status, HttpRequest request) {
		super(status, request);
	}

	// Overwrites the standard Controller run method to handle devices afterwards
	public void run() {
		super.run();

		try {
			// Wait 3 seconds before marking this connection as closed, to give the client time to re-establish a long poll
			Thread.sleep(3000);
		} catch (InterruptedException e) {}
		status.getDeviceList().closeConnection(request);

	}

	protected void processRequest() throws IOException, UnsupportedEncodingException, InterruptedException {
		update_success = updatePlaylist(status.getPlaylist());
		device = status.getDeviceList().openConnection(request);
		respond();
		request.close();
	}

	private void respond() throws IOException, UnsupportedEncodingException, InterruptedException {
		Gson gson = CustomGson.get(status);
		if (request.getPath().equals("/poll/summary")) {
			long startTime = System.nanoTime();
			int hashcode;
			try {
				hashcode = Integer.parseInt(request.getParam("hashcode"));
			} catch (NumberFormatException e) {
				hashcode = 0;
			}
			while (true) {
				if (status.summaryHasChanged(hashcode)) {
					request.sendHeaders(200, "Long Poll", "application/json");
					request.writeBody(gson.toJson(status.getSummary()));
					break;
				}
				if ((System.nanoTime() - startTime) > (1000000000L * 30)) {
					request.sendHeaders(200, "Long Poll", "application/json");
					request.writeBody("{ }");
					break;
				}
				Thread.sleep(1);
			}
		} else if (request.getPath().equals("/play") && request.getMethod() == Method.POST) {
			status.setPlaying(true);
			request.sendHeaders(204, "Changed");
		} else if (request.getPath().equals("/pause") && request.getMethod() == Method.POST) {
			status.setPlaying(false);
			request.sendHeaders(204, "Changed");
		} else if (request.getPath().equals("/volume") && request.getMethod() == Method.POST) {
			float volume = parseVolume();
			if (volume != -1) {
				status.setVolume(volume);
				request.sendHeaders(204, "Changed");
			}
		} else if (request.getPath().equals("/next") && request.getMethod() == Method.POST) {
			String expectedNowUrl = request.getParam("now");
			if (expectedNowUrl == null) {
				status.getPlaylist().skipTrack();
				request.sendHeaders(204, "Changed");
			} else {
				Track nowTrack = new Track(status.getMediaApi(), expectedNowUrl);
				if (status.getPlaylist().skipTrack(nowTrack)) {
					request.sendHeaders(204, "Changed");
				} else {
					request.sendHeaders(404, "Track Not In Playlist");
				}
			}
		} else if (request.getPath().equals("/done") && request.getMethod() == Method.POST) {
			String oldTrackUrl = request.getParam("track");
			if (oldTrackUrl == null) {
				request.sendHeaders(400, "Missing `track` parameter");
			} else {
				Track oldTrack = new Track(status.getMediaApi(), oldTrackUrl);
				if (status.getPlaylist().completeTrack(oldTrack)) {
					request.sendHeaders(204, "Changed");
				} else {
					request.sendHeaders(404, "Track Not In Playlist");
				}
			}
		} else if (request.getPath().equals("/error") && request.getMethod() == Method.POST) {
			String oldTrackUrl = request.getParam("track");
			String errorMessage = request.getParam("message");
			if (oldTrackUrl == null) {
				request.sendHeaders(400, "Missing `track` parameter");
			} else if (errorMessage == null) {
				request.sendHeaders(400, "Missing `message` parameter");
			} else {
				Track oldTrack = new Track(status.getMediaApi(), oldTrackUrl);
				if (status.getPlaylist().flagTrackAsError(oldTrack, errorMessage)) {
					request.sendHeaders(204, "Changed");
				} else {
					request.sendHeaders(404, "Track Not In Playlist");
				}
			}
		} else if (request.getPath().equals("/update") && request.getMethod() == Method.POST) {
			
			// All the work has already been done, so just return headers based on result.
			if (update_success) request.sendHeaders(204, "Changed");
			else request.sendHeaders(400, "Incorrect params");
		} else if (request.getPath().equals("/queue") && request.getMethod() == Method.POST) {
			String url = request.getParam("url", "");
			String pos = request.getParam("pos", "end");
			Track newTrack = new Track(status.getMediaApi(), url);
			if (pos.equals("now")) {
				status.getPlaylist().queueNow(newTrack);
				status.setPlaying(true);
			} else if (pos.equals("next")) {
				status.getPlaylist().queueNext(newTrack);
			} else {
				status.getPlaylist().queueEnd(newTrack);
			}
			request.sendHeaders(204, "Queued");

			// The queued track is unlikely to include a full set of metadata
			// So do a refresh to get the latest (but don't let that block any of the above queuing action)
			newTrack.refreshMetadata();
		} else if (request.getPath().equals("/collection") && request.getMethod() == Method.POST) {
			String collectionSlug = request.getParam("slug");
			Fetcher fetcher;
			if (collectionSlug == null || collectionSlug.equals("")) {
				fetcher = new RandomFetcher();
			} else {
				fetcher = new CollectionFetcher(collectionSlug);
			}
			status.getPlaylist().setFetcher(fetcher);
			request.sendHeaders(204, "Changed");
		} else if (request.getPath().equals("/devices") && request.getMethod() == Method.POST) {
			status.getDeviceList().updateDevice(request.getParam("uuid"), request.getParam("name"));
			request.sendHeaders(204, "Changed");
		} else if (request.getPath().equals("/devices/current") && request.getMethod() == Method.POST) {
			status.getDeviceList().setCurrent(request.getParam("uuid"));
			if (request.getParam("play", "false").equals("true")) status.setPlaying(true);
			request.sendHeaders(204, "Changed");
		} else if(request.getPath().equals("/_info")) {
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
		} else if(request.getPath().equals("/robots.txt")) {
			request.sendHeaders(200, "OK", "text/plain");
			request.writeBody("User-agent: *\nDisallow: /\n");
		} else if(request.getPath().equals("/trackUpdated") && request.getMethod() == Method.POST) {
			try {
				LoganneTrackEvent event = gson.fromJson(request.getData(), LoganneTrackEvent.class);
				status.getPlaylist().updateTracks(event.track.getMetadata("trackid"), event.track);
				request.sendHeaders(204, "No Content");
			} catch (JsonSyntaxException exception) {
				request.sendHeaders(400, "Bad Request");
				request.writeBody("JSON Syntax Error");
				request.writeBody(exception.getMessage());
			}
		} else if(request.getPath().equals("/trackDeleted") && request.getMethod() == Method.POST) {
			try {
				LoganneTrackEvent event = gson.fromJson(request.getData(), LoganneTrackEvent.class);
				status.getPlaylist().deleteTrack(event.track);
				request.sendHeaders(204, "No Content");
			} catch (JsonSyntaxException exception) {
				request.sendHeaders(400, "Bad Request");
				request.writeBody("JSON Syntax Error");
				request.writeBody(exception.getMessage());
			}
		} else if(request.getPath().equals("/collectionsChanged") && request.getMethod() == Method.POST) {
			// If anything has changed with collections (added, removed, edited etc), it's easier to refresh the full list than track the changes.
			if (status.getCollectionList().refreshList()) {
				request.sendHeaders(204, "No Content");
			} else {
				request.sendHeaders(500, "Internal Server Error");
				request.writeBody("Failed to fetch collections from media API");
			}
		} else {
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
			} else {
				System.err.println("WARNING: File Not found: ".concat(request.getPath()));
				request.sendHeaders(404, "Not Found", "text/plain");
				request.writeBody("File Not Found");
			}
		}
	}
	private float parseVolume() throws IOException {
		try{
			float value = Float.parseFloat(request.getParam("volume"));
			if (!(value <= 1.0)) {
				request.sendHeaders(400, "Not Changed", "application/json");
				request.writeBody("Volume must not be greater than 1.0");
				return -1;
			} else if (!(value >= 0.0)) {
				request.sendHeaders(400, "Not Changed", "application/json");
				request.writeBody("Volume must not be less than 0.0");
				return -1;
			}
			return value;
		} catch (NullPointerException e) {
			request.sendHeaders(400, "Not Changed", "application/json");
			request.writeBody("Volume GET parameter must be set");
		} catch (NumberFormatException e) {
			request.sendHeaders(400, "Not Changed", "application/json");
			request.writeBody("Volume must be a number");
		}
		return -1;
	}

	/**
	 * Updates a the time for a track in the given playlist
	 * 
	 * Returns a boolean of whether the update was succesful
	 **/
	private boolean updatePlaylist(Playlist playlist) {
		String update_url = request.removeParam("update_url");
		String update_time = request.removeParam("update_time");
		String update_timeset = request.removeParam("update_timeset");
		boolean update_success = false;
		if (update_url != null && update_time != null) {
			try{
				Track update_curtrack = new Track(status.getMediaApi(), update_url);
				float update_currentTime = Float.parseFloat(update_time);
				BigInteger update_currentTimeSet = new BigInteger(update_timeset);
				update_success = playlist.setTrackTime(update_curtrack, update_currentTime, update_currentTimeSet);
			} catch (Exception e) {
				System.err.println("ERROR: Unexpected error updating manager");
				e.printStackTrace();
			}
		}
		return update_success;
	}

}