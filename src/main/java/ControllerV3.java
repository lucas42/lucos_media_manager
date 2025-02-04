import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
class ControllerV3 extends Controller {
	public ControllerV3(Status status, HttpRequest request) {
		super(status, request);
	}
	protected void processRequest() throws IOException {
		if (request.isAuthorised() || request.getMethod().equals(Method.OPTIONS)) {
			String[] pathParts = request.getPath().split("/");
			if (request.getPath().equals("/v3/is-playing")) {
				if (request.getMethod().equals(Method.PUT)) {
					if (request.getData().toLowerCase().equals("true")) {
						status.setPlaying(true);
						request.sendHeaders(204, "Changed");
						request.close();
					} else if (request.getData().toLowerCase().equals("false")) {
						status.setPlaying(false);
						request.sendHeaders(204, "Changed");
						request.close();
					} else {
						request.sendHeaders(400, "Not Changed", "text/plain");
						request.writeBody("Unknown value \"".concat(request.getData()).concat("\""));
						request.close();
					}
				} else {
					request.notAllowed(Arrays.asList(Method.PUT));
				}
			} else if (request.getPath().equals("/v3/volume")) {
				if (request.getMethod().equals(Method.PUT)) {
					if (request.getData() == "") {
						request.sendHeaders(400, "Not Changed", "text/plain");
						request.writeBody("Request body must be set to value for volume");
						request.close();
					} else {
						try{
							float value = Float.parseFloat(request.getData());
							if (!(value <= 1.0)) {
								request.sendHeaders(400, "Not Changed", "text/plain");
								request.writeBody("Volume must not be greater than 1.0");
								request.close();
							} else if (!(value >= 0.0)) {
								request.sendHeaders(400, "Not Changed", "text/plain");
								request.writeBody("Volume must not be less than 0.0");
								request.close();
							} else {
								status.setVolume(value);
								request.sendHeaders(204, "Changed");
								request.close();
							}
						} catch (NumberFormatException e) {
							request.sendHeaders(400, "Not Changed", "text/plain");
							request.writeBody("Volume must be a number");
							request.close();
						}
					}
				} else {
					request.notAllowed(Arrays.asList(Method.PUT));
				}

			} else if (request.getPath().startsWith("/v3/device-names/")) {
				if (request.getMethod().equals(Method.PUT)) {
					String uuid = request.getPath().replace("/v3/device-names/", "");
					status.getDeviceList().updateDevice(uuid, request.getData());
					request.sendHeaders(204, "Changed");
					request.close();
				} else {
					request.notAllowed(Arrays.asList(Method.PUT));
				}
			} else if (request.getPath().equals("/v3/current-device")) {
				if (request.getMethod().equals(Method.PUT)) {
					status.getDeviceList().setCurrent(request.getData());
					request.sendHeaders(204, "Changed");
					request.close();
				} else {
					request.notAllowed(Arrays.asList(Method.PUT));
				}
			} else if (request.getPath().startsWith("/v3/playlist/") && (pathParts.length >= 5)) {
				String playlistSlug = pathParts[3]; // TODO: check the playlist slug matches the playlist.  For now, always uses the current playlist, regardless of this slug
				String trackUuid = pathParts[4];
				String trackAspect = (pathParts.length > 5) ? pathParts[5] : null;
				if (trackAspect == null) {
					if (request.getMethod().equals(Method.DELETE)) {
						String action = request.getParam("action");
						if (action == null) {
							request.sendHeaders(400, "Bad Request", "text/plain");
							request.writeBody("Missing required `action` GET parameter.  Must be one of: complete, error, skip");
							request.close();
						} else if (action.equals("complete")) {
							if (status.getPlaylist().completeTrack(trackUuid)) {
								request.sendHeaders(204, "Changed");
								request.close();
							} else {
								request.sendHeaders(204, "Not Changed");
								request.close();
							}
						} else if (action.equals("error")) {
							String errorMessage = request.getData();
							if (errorMessage.equals("")) {
								request.sendHeaders(400, "Bad Request", "text/plain");
								request.writeBody("Missing error message from request body");
								request.close();
							} else {
								if (status.getPlaylist().flagTrackAsError(trackUuid, errorMessage)) {
									request.sendHeaders(204, "Changed");
									request.close();
								} else {
									request.sendHeaders(204, "Not Changed");
									request.close();
								}
							}
						} else if (action.equals("skip")) {
							if (status.getPlaylist().skipTrack(trackUuid)) {
								request.sendHeaders(204, "Changed");
								request.close();
							} else {
								request.sendHeaders(204, "Not Changed");
								request.close();
							}
						} else {
							request.sendHeaders(400, "Bad Request", "text/plain");
							request.writeBody("Unknown `action` GET parameter \""+action+"\".  Must be one of: complete, error, skip");
							request.close();
						}
					} else {
						request.notAllowed(Arrays.asList(Method.DELETE));
					}
				} else if (trackAspect.equals("current-time")) {
					if (request.getMethod().equals(Method.PUT)) {
						try{
							float trackTime = Float.parseFloat(request.getData());
							if (status.getPlaylist().setTrackTimeByUuid(trackUuid, trackTime)) {
								request.sendHeaders(204, "Changed");
								request.close();
							} else {
								request.sendHeaders(204, "Not Changed");
								request.close();
							}
						} catch (NumberFormatException e) {
							request.sendHeaders(400, "Bad Request", "text/plain");
							request.writeBody("Invalid time given \""+request.getData()+"\".  Must be a number");
							request.close();
						}
					} else {
						request.notAllowed(Arrays.asList(Method.PUT));
					}
				} else {
					System.err.println("WARNING: File Not found: ".concat(request.getPath()));
					request.sendHeaders(404, "Not Found", "text/plain");
					request.writeBody("File Not Found");
					request.close();
				}
			} else if (request.getPath().equals("/v3/skip-track")) {
				if (request.getMethod().equals(Method.POST)) {
					status.getPlaylist().skipTrack();
					request.sendHeaders(204, "Changed");
					request.close();
				} else {
					request.notAllowed(Arrays.asList(Method.POST));
				}
			} else if (request.getPath().equals("/v3/queue-track")) {
				if (request.getMethod().equals(Method.POST)) {
					if (request.getData() == "") {
						request.sendHeaders(400, "Bad Request", "text/plain");
						request.writeBody("Missing track url from request body");
						request.close();
					} else {
						Track newTrack = new Track(status.getMediaApi(), request.getData());
						String position = request.getParam("position", "end");
						if (position.equals("now")) {
							status.getPlaylist().queueNow(newTrack);
							status.setPlaying(true);
						} else if (position.equals("next")) {
							status.getPlaylist().queueNext(newTrack);
						} else {
							status.getPlaylist().queueEnd(newTrack);
						}
						request.sendHeaders(204, "Changed");
						request.close();

						// The queued track won't include a full set of metadata
						// So do a refresh to get the latest (but don't let that block any of the above queuing action)
						newTrack.refreshMetadata();
					}
				} else {
					request.notAllowed(Arrays.asList(Method.POST));
				}
			} else if (request.getPath().equals("/v3/current-collection")) {
				if (request.getMethod().equals(Method.PUT)) {
					if (request.getData().equals(status.getPlaylist().getCurrentFetcherSlug())) {
						request.sendHeaders(204, "Not Changed");
						request.close();
					} else {
						Fetcher fetcher = Fetcher.createFromSlug(status.getMediaApi(), request.getData());
						fetcher.setPostFetchFunction(status::syncToFileSystem);
						status.getPlaylist().setFetcher(fetcher);
						status.getPlaylist().topupTracks();
						request.sendHeaders(204, "Changed");
						request.close();
					}
				} else {
					request.notAllowed(Arrays.asList(Method.PUT));
				}
			} else {
				System.err.println("WARNING: File Not found: ".concat(request.getPath()));
				request.sendHeaders(404, "Not Found", "text/plain");
				request.writeBody("File Not Found");
				request.close();
			}
		} else {
			request.sendHeaders(401, "Unauthorized",  Map.of(
				"Content-Type", "text/plain",
				"WWW-Authenticate", "key"
			));
			request.writeBody("Invalid API Key");
			request.close();
		}
	}
}