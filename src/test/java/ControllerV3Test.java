import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;
import java.util.Map;
import java.util.Collection;
import java.util.Arrays;
import java.util.HashMap;
import java.io.IOException;

class ControllerV3Test {
	void compareRequestResponse(Status status, String path, Method method, Map<String, String> getParameters, String requestBody, int responseStatus, String responseString, String contentType, String responseBody) throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		if (getParameters != null) {
			for (String paramKey : getParameters.keySet()) {
				when(request.getParam(paramKey, "end")).thenReturn(getParameters.getOrDefault(paramKey, "end"));  // HACK: hardcoding "end" here, as that's the default for queue position.  Currently it's the only v3 endpoint relying on GET params, so it kinda works.
				when(request.getParam(paramKey)).thenReturn(getParameters.get(paramKey));
			}
		}
		when(request.getPath()).thenReturn(path);
		when(request.getMethod()).thenReturn(method);
		when(request.isAuthorised()).thenReturn(true);
		if (requestBody == null) requestBody = "";
		when(request.getData()).thenReturn(requestBody);
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).isAuthorised();
		if (contentType != null) verify(request).sendHeaders(responseStatus, responseString, contentType);
		else verify(request).sendHeaders(responseStatus, responseString);
		if (responseBody != null) verify(request).writeBody(responseBody);
		verify(request).close();
	}
	void checkNotAllowed(Status status, String path, Method method, Map<String, String> getParameters, Collection<Method> allowedMethods, boolean isAuthorised) throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn(path);
		when(request.getMethod()).thenReturn(method);
		when(request.isAuthorised()).thenReturn(isAuthorised);
		Controller controller = new FrontController(status, request);
		controller.run();

		verify(request).notAllowed(allowedMethods);
	}

	@Test
	void unknownPathReturns404() throws Exception {
		Status status = new Status(mock(Playlist.class), mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		compareRequestResponse(status, "/v3/unknown", Method.GET, null, null, 404, "Not Found", "text/plain", "File Not Found");
		compareRequestResponse(status, "/v3", Method.GET, null, null, 404, "Not Found", "text/plain", "File Not Found");
		compareRequestResponse(status, "/v3/playlist/slug/uuid/unknown", Method.GET, null, null, 404, "Not Found", "text/plain", "File Not Found");
	}

	@Test
	void playingPause() throws Exception {
		Status status = new Status(mock(Playlist.class), mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		status.setPlaying(true);
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, null, "False", 204, "Changed", null, null);
		assertFalse(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, null, "False", 204, "Changed", null, null);
		assertFalse(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, null, "True", 204, "Changed", null, null);
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, null, "True", 204, "Changed", null, null);
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, null, "Unknown", 400, "Not Changed", "text/plain", "Unknown value \"Unknown\""); // Test unknown value doesn't change anything
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, null, "fAlSe", 204, "Changed", null, null); // Test case insensitive
		assertFalse(status.getPlaying());
		checkNotAllowed(status, "/v3/is-playing", Method.POST, null, Arrays.asList(Method.PUT), true);
		assertFalse(status.getPlaying());
	}

	@Test
	void volume() throws Exception {
		Status status = new Status(mock(Playlist.class), mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));

		// The following values should update the volume
		compareRequestResponse(status, "/v3/volume", Method.PUT, null, "1.0", 204, "Changed", null, null);
		assertEquals(1, status.getVolume(), 0);
		compareRequestResponse(status, "/v3/volume", Method.PUT, null, "0", 204, "Changed", null, null);
		assertEquals(0, status.getVolume(), 0);
		compareRequestResponse(status, "/v3/volume", Method.PUT, null, "0.7", 204, "Changed", null, null);
		assertEquals(0.7, status.getVolume(), 0.0002);

		// The following values should all fail, and not update the volume
		compareRequestResponse(status, "/v3/volume", Method.PUT, null, null, 400, "Not Changed", "text/plain", "Request body must be set to value for volume");
		compareRequestResponse(status, "/v3/volume", Method.PUT, null, "string", 400, "Not Changed", "text/plain", "Volume must be a number");
		compareRequestResponse(status, "/v3/volume", Method.PUT, null, "NaN", 400, "Not Changed", "text/plain", "Volume must not be greater than 1.0");
		compareRequestResponse(status, "/v3/volume", Method.PUT, null, "-1", 400, "Not Changed", "text/plain", "Volume must not be less than 0.0");
		compareRequestResponse(status, "/v3/volume", Method.PUT, null, "1.3", 400, "Not Changed", "text/plain", "Volume must not be greater than 1.0");
		checkNotAllowed(status, "/v3/volume", Method.POST, null, Arrays.asList(Method.PUT), true);
		assertEquals(0.7, status.getVolume(), 0.0002);
	}

	@Test
	void setDeviceName() throws Exception {
		Status status = new Status(mock(Playlist.class), new DeviceList(mock(Loganne.class)), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));

		// Create a device if none exists, and set its name
		compareRequestResponse(status, "/v3/device-names/47d9cba3-fd9f-445d-b984-072e4f75732c", Method.PUT, null, "Home Laptop", 204, "Changed", null, null);
		assertEquals("Home Laptop", status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").getName()); // Device created and given a name
		compareRequestResponse(status, "/v3/device-names/a7441bd5-65a1-4357-bc96-c0ece53def07", Method.PUT, null, "Phone", 204, "Changed", null, null);
		assertEquals("Phone", status.getDeviceList().getDevice("a7441bd5-65a1-4357-bc96-c0ece53def07").getName()); // Device created and given a name
		assertEquals("Home Laptop", status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").getName()); // Existing device unaffected
		compareRequestResponse(status, "/v3/device-names/47d9cba3-fd9f-445d-b984-072e4f75732c", Method.PUT, null, "Personal Laptop", 204, "Changed", null, null);
		assertEquals("Personal Laptop", status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").getName()); // Existing device's name updated
		assertEquals("Phone", status.getDeviceList().getDevice("a7441bd5-65a1-4357-bc96-c0ece53def07").getName()); // Existing divec unnafected

		checkNotAllowed(status, "/v3/device-names/47d9cba3-fd9f-445d-b984-072e4f75732c", Method.POST, null, Arrays.asList(Method.PUT), true);
	}

	@Test
	void setCurrentDevice() throws Exception {
		Loganne loganne = mock(Loganne.class);
		Status status = new Status(mock(Playlist.class), new DeviceList(loganne), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));

		// Create a device if none exists, and set its name
		compareRequestResponse(status, "/v3/current-device", Method.PUT, null, "47d9cba3-fd9f-445d-b984-072e4f75732c", 204, "Changed", null, null);
		assertTrue(status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").isCurrent()); // Device created and set as current one
		verify(loganne).post("deviceSwitch", "Playing music on first device connected");
		compareRequestResponse(status, "/v3/current-device", Method.PUT, null, "a7441bd5-65a1-4357-bc96-c0ece53def07", 204, "Changed", null, null);
		assertTrue(status.getDeviceList().getDevice("a7441bd5-65a1-4357-bc96-c0ece53def07").isCurrent()); // Device created and set as current one
		assertFalse(status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").isCurrent()); // Existing device marked as not current
		verify(loganne).post("deviceSwitch", "Moving music to play on Device 2");
		compareRequestResponse(status, "/v3/current-device", Method.PUT, null, "47d9cba3-fd9f-445d-b984-072e4f75732c", 204, "Changed", null, null);
		assertTrue(status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").isCurrent()); // Existing device set as current one
		assertFalse(status.getDeviceList().getDevice("a7441bd5-65a1-4357-bc96-c0ece53def07").isCurrent()); // Existing device marked as not current
		verify(loganne).post("deviceSwitch", "Moving music to play on Device 1");

		checkNotAllowed(status, "/v3/current-device", Method.POST, null, Arrays.asList(Method.PUT), true);

	}

	@Test
	void trackComplete() throws Exception {
		Fetcher fetcher = mock(Fetcher.class);
		when(fetcher.getSlug()).thenReturn("special");

		// Create playlist with 4 tracks, 2 of which are the same track
		Playlist playlist = new Playlist(fetcher, null);
		Status status = new Status(playlist, mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		Track trackA = new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347")));
		Track trackB = new Track(status.getMediaApi(), "http://example.com/track/8532", new HashMap<String, String>(Map.of("title", "Good as Gold", "artist", "Beautiful South", "trackid", "8532")));
		Track trackC = new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347")));
		Track trackD = new Track(status.getMediaApi(), "http://example.com/track/8533", new HashMap<String, String>(Map.of("title", "Old Red Eyes Is Back", "artist", "Beautiful South", "trackid", "8533")));
		playlist.queue(new Track[]{trackA, trackB, trackC, trackD});

		int hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackA.getUuid(), Method.DELETE, Map.of("action", "complete"), null, 204, "Changed", null, null);  // Removes the first instance of the track

		assertEquals(3, playlist.getLength());
		assertNotEquals("Stairway To Heaven", playlist.getCurrentTrack().getMetadata("title"));
		assertEquals("Stairway To Heaven", playlist.getNextTrack().getMetadata("title"));
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackC.getUuid(), Method.DELETE, Map.of("action", "complete"), null, 204, "Changed", null, null);  // Removes the second instance of the track
		assertEquals(2, playlist.getLength());
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackA.getUuid(), Method.DELETE, Map.of("action", "complete"), null, 204, "Not Changed", null, null);  // No instances of track remain - no-op
		assertEquals(2, playlist.getLength());
		assertEquals(hashCode, status.hashCode());

		compareRequestResponse(status, "/v3/playlist/special/"+trackD.getUuid(), Method.DELETE, null, "", 400, "Bad Request", "text/plain", "Missing required `action` GET parameter.  Must be one of: complete, error, skip");  // Unknown action specified - no-op
		assertEquals(2, playlist.getLength());
		assertEquals(hashCode, status.hashCode());

		compareRequestResponse(status, "/v3/playlist/special/"+trackD.getUuid(), Method.DELETE, Map.of("action", "jump"), "", 400, "Bad Request", "text/plain", "Unknown `action` GET parameter \"jump\".  Must be one of: complete, error, skip");  // Unknown action specified - no-op
		assertEquals(2, playlist.getLength());
		assertEquals(hashCode, status.hashCode());

		checkNotAllowed(status, "/v3/playlist/special/"+trackD.getUuid(), Method.PUT, Map.of("action", "complete"), Arrays.asList(Method.DELETE), true);

		compareRequestResponse(status, "/v3/playlist/special/", Method.DELETE, null, null, 404, "Not Found", "text/plain", "File Not Found");
	}

	@Test
	void trackError() throws Exception {
		Fetcher fetcher = mock(Fetcher.class);
		when(fetcher.getSlug()).thenReturn("special");

		// Create playlist with 4 tracks, 2 of which are the same track
		Playlist playlist = new Playlist(fetcher, null);
		Status status = new Status(playlist, mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		Track trackA = new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347")));
		Track trackB = new Track(status.getMediaApi(), "http://example.com/track/8532", new HashMap<String, String>(Map.of("title", "Good as Gold", "artist", "Beautiful South", "trackid", "8532")));
		Track trackC = new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347")));
		Track trackD = new Track(status.getMediaApi(), "http://example.com/track/8533", new HashMap<String, String>(Map.of("title", "Old Red Eyes Is Back", "artist", "Beautiful South", "trackid", "8533")));
		playlist.queue(new Track[]{trackA, trackB, trackC, trackD});

		int hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackA.getUuid(), Method.DELETE, Map.of("action", "error"), "Failed to load track", 204, "Changed", null, null);  // Removes the first instance of the track

		assertEquals(3, playlist.getLength());
		assertNotEquals("Stairway To Heaven", playlist.getCurrentTrack().getMetadata("title"));
		assertEquals("Stairway To Heaven", playlist.getNextTrack().getMetadata("title"));
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackC.getUuid(), Method.DELETE, Map.of("action", "error"), "Tracked cut out early", 204, "Changed", null, null);  // Removes the second instance of the track
		assertEquals(2, playlist.getLength());
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackA.getUuid(), Method.DELETE, Map.of("action", "error"), "Failed to load track", 204, "Not Changed", null, null);  // No instances of track remain - no-op
		assertEquals(2, playlist.getLength());
		assertEquals(hashCode, status.hashCode());

		compareRequestResponse(status, "/v3/playlist/special/"+trackA.getUuid(), Method.DELETE, Map.of("action", "error"), "", 400, "Bad Request", "text/plain", "Missing error message from request body");  // No message specified - no-op
		assertEquals(2, playlist.getLength());
		assertEquals(hashCode, status.hashCode());

		checkNotAllowed(status, "/v3/playlist/special/"+trackD.getUuid(), Method.PUT, Map.of("action", "error"), Arrays.asList(Method.DELETE), true);

	}


	@Test
	void skipTrack() throws Exception {
		Fetcher fetcher = mock(Fetcher.class);
		when(fetcher.getSlug()).thenReturn("special");

		// Create playlist with 4 tracks, 2 of which are the same track
		Playlist playlist = new Playlist(fetcher, null);
		Status status = new Status(playlist, mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		Track trackA = new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347")));
		Track trackB = new Track(status.getMediaApi(), "http://example.com/track/8532", new HashMap<String, String>(Map.of("title", "Good as Gold", "artist", "Beautiful South", "trackid", "8532")));
		Track trackC = new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347")));
		Track trackD = new Track(status.getMediaApi(), "http://example.com/track/8533", new HashMap<String, String>(Map.of("title", "Old Red Eyes Is Back", "artist", "Beautiful South", "trackid", "8533")));
		playlist.queue(new Track[]{trackA, trackB, trackC, trackD});

		int hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackA.getUuid(), Method.DELETE, Map.of("action", "skip"), null, 204, "Changed", null, null);  // Removes the first instance of the track

		assertEquals(3, playlist.getLength());
		assertNotEquals("Stairway To Heaven", playlist.getCurrentTrack().getMetadata("title"));
		assertEquals("Stairway To Heaven", playlist.getNextTrack().getMetadata("title"));
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackC.getUuid(), Method.DELETE, Map.of("action", "skip"), null, 204, "Changed", null, null);  // Removes the second instance of the track
		assertEquals(2, playlist.getLength());
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackA.getUuid(), Method.DELETE, Map.of("action", "skip"), null, 204, "Not Changed", null, null);  // No instances of track remain - no-op
		assertEquals(2, playlist.getLength());
		assertEquals(hashCode, status.hashCode());

		compareRequestResponse(status, "/v3/skip-track", Method.POST, null, null, 204, "Changed", null, null);  // Separate endpoint for when track isn't known
		assertEquals(1, playlist.getLength());
		assertNotEquals(hashCode, status.hashCode());

		checkNotAllowed(status, "/v3/skip-track", Method.PUT, null, Arrays.asList(Method.POST), true);

	}

	@Test
	void skipLaterInstanceOfDuplicateTrack() throws Exception {
		Fetcher fetcher = mock(Fetcher.class);
		when(fetcher.getSlug()).thenReturn("special");

		// Create playlist with 4 tracks, 2 of which are the same track
		Playlist playlist = new Playlist(fetcher, null);
		Status status = new Status(playlist, mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		Track trackA = new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347")));
		Track trackB = new Track(status.getMediaApi(), "http://example.com/track/8532", new HashMap<String, String>(Map.of("title", "Good as Gold", "artist", "Beautiful South", "trackid", "8532")));
		Track trackC = new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347")));
		Track trackD = new Track(status.getMediaApi(), "http://example.com/track/8533", new HashMap<String, String>(Map.of("title", "Old Red Eyes Is Back", "artist", "Beautiful South", "trackid", "8533")));
		playlist.queue(new Track[]{trackA, trackB, trackC, trackD});

		int hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/special/"+trackC.getUuid(), Method.DELETE, Map.of("action", "skip"), null, 204, "Changed", null, null);  // Removes the second instance of this track

		assertEquals(3, playlist.getLength());
		assertEquals("Stairway To Heaven", playlist.getCurrentTrack().getMetadata("title"));
		assertNotEquals("Stairway To Heaven", playlist.getNextTrack().getMetadata("title"));
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

	}

	@Test
	void queueTrack() throws Exception {
		Fetcher fetcher = mock(RandomFetcher.class);

		Playlist playlist = new Playlist(fetcher, null);
		MediaApi mediaApi = mock(MediaApi.class);
		Status status = new Status(playlist, mock(DeviceList.class), mock(CollectionList.class), mediaApi, mock(FileSystemSync.class));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347"))));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/8532", new HashMap<String, String>(Map.of("title", "Good as Gold", "artist", "Beautiful South", "trackid", "8532"))));

		status.setPlaying(false);
		int hashCode = status.hashCode();

		when(mediaApi.fetchTrack("/v2/tracks?url=http%3A%2F%2Fexample.com%2Ftrack%2F1234")).thenReturn(new Track(status.getMediaApi(), "http://example.com/track/1234", new HashMap<String, String>(Map.of("title", "One Two Three Four", "trackid", "1234"))));
		compareRequestResponse(status, "/v3/queue-track", Method.POST, Map.of("position", "now"), "http://example.com/track/1234", 204, "Changed", null, null);  // Adds a track to the start of the playlist
		assertEquals(3, playlist.getLength());
		assertEquals("http://example.com/track/1234", playlist.getCurrentTrack().getUrl());
		assertEquals("One Two Three Four", playlist.getCurrentTrack().getMetadata("title"));
		assertNotEquals(hashCode, status.hashCode());
		assertTrue(status.getPlaying()); // Queuing a track now, should start playing automatically
		hashCode = status.hashCode();
		verify(mediaApi).fetchTrack("/v2/tracks?url=http%3A%2F%2Fexample.com%2Ftrack%2F1234");

		when(mediaApi.fetchTrack("/v2/tracks?url=http%3A%2F%2Fexample.com%2Ftrack%2F2468")).thenReturn(new Track(status.getMediaApi(), "http://example.com/track/2468", new HashMap<String, String>(Map.of("title", "Evens", "trackid", "2468"))));
		compareRequestResponse(status, "/v3/queue-track", Method.POST, Map.of("position", "next"), "http://example.com/track/2468", 204, "Changed", null, null);  // Adds a track to second place in the playlist
		assertEquals(4, playlist.getLength());
		assertEquals("http://example.com/track/2468", playlist.getNextTrack().getUrl());
		assertEquals("Evens", playlist.getNextTrack().getMetadata("title"));
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();
		verify(mediaApi).fetchTrack("/v2/tracks?url=http%3A%2F%2Fexample.com%2Ftrack%2F2468");

		when(mediaApi.fetchTrack("/v2/tracks?url=http%3A%2F%2Fexample.com%2Ftrack%2F3579")).thenReturn(new Track(status.getMediaApi(), "http://example.com/track/3579", new HashMap<String, String>(Map.of("title", "Oddz", "trackid", "3579"))));
		compareRequestResponse(status, "/v3/queue-track", Method.POST, Map.of("position", "end"), "http://example.com/track/3579", 204, "Changed", null, null);  // Adds a track to the end of the playlist
		assertEquals(5, playlist.getLength());
		assertEquals("http://example.com/track/3579", playlist.getTracks().get(playlist.getLength()-1).getUrl());
		assertEquals("Oddz", playlist.getTracks().get(playlist.getLength()-1).getMetadata("title"));
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();
		verify(mediaApi).fetchTrack("/v2/tracks?url=http%3A%2F%2Fexample.com%2Ftrack%2F3579");

		when(mediaApi.fetchTrack("/v2/tracks?url=http%3A%2F%2Fexample.com%2Ftrack%2F9876")).thenReturn(new Track(status.getMediaApi(), "http://example.com/track/9876", new HashMap<String, String>(Map.of("title", "Downwards", "trackid", "9876"))));
		compareRequestResponse(status, "/v3/queue-track", Method.POST, Map.of("position", ""), "http://example.com/track/9876", 204, "Changed", null, null);  // Adds a track to the end of the playlist
		assertEquals(6, playlist.getLength());
		assertEquals("http://example.com/track/9876", playlist.getTracks().get(playlist.getLength()-1).getUrl());
		assertEquals("Downwards", playlist.getTracks().get(playlist.getLength()-1).getMetadata("title"));
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();
		verify(mediaApi).fetchTrack("/v2/tracks?url=http%3A%2F%2Fexample.com%2Ftrack%2F9876");

		compareRequestResponse(status, "/v3/queue-track", Method.POST, null, null, 400, "Bad Request", "text/plain", "Missing track url from request body");  // No track specified - no-op
		assertEquals(6, playlist.getLength());
		assertEquals(hashCode, status.hashCode());

		checkNotAllowed(status, "/v3/queue-track", Method.PUT, null, Arrays.asList(Method.POST), true);
	}

	@Test
	void setCurrentCollection() throws Exception {
		try (MockedStatic mockedAbstractFetcher = mockStatic(Fetcher.class)) {
			MediaApi api = mock(MediaApi.class);
			Fetcher robotFetcher = mock(CollectionFetcher.class);
			when(robotFetcher.getSlug()).thenReturn("robots");
			Fetcher allFetcher = mock(RandomFetcher.class);
			when(allFetcher.getSlug()).thenReturn("all");
			mockedAbstractFetcher.when(() -> Fetcher.createFromSlug(api, "robots")).thenReturn(robotFetcher);
			mockedAbstractFetcher.when(() -> Fetcher.createFromSlug(api, "all")).thenReturn(allFetcher);
			Playlist playlist = new Playlist(mock(RandomFetcher.class), null);
			Status status = new Status(playlist, mock(DeviceList.class), mock(CollectionList.class), api, mock(FileSystemSync.class));

			compareRequestResponse(status, "/v3/current-collection", Method.PUT, null, "robots", 204, "Changed", null, null);
			assertEquals("robots", playlist.getCurrentFetcherSlug());
			compareRequestResponse(status, "/v3/current-collection", Method.PUT, null, "robots", 204, "Not Changed", null, null);
			assertEquals("robots", playlist.getCurrentFetcherSlug());
			compareRequestResponse(status, "/v3/current-collection", Method.PUT, null, "all", 204, "Changed", null, null);
			assertEquals("all", playlist.getCurrentFetcherSlug());

			checkNotAllowed(status, "/v3/current-collection", Method.POST, null, Arrays.asList(Method.PUT), true);
		}
	}

	@Test
	void setCollectionToCurrentHasNoImpact() throws Exception {
		try (MockedStatic mockedAbstractFetcher = mockStatic(Fetcher.class)) {
			MediaApi api = mock(MediaApi.class);
			Fetcher relaxFetcher = mock(CollectionFetcher.class);
			when(relaxFetcher.getSlug()).thenReturn("relax");
			Fetcher allFetcher = mock(RandomFetcher.class);
			when(allFetcher.getSlug()).thenReturn("all");
			mockedAbstractFetcher.when(() -> Fetcher.createFromSlug(api, "relax")).thenReturn(relaxFetcher);
			mockedAbstractFetcher.when(() -> Fetcher.createFromSlug(api, "all")).thenReturn(allFetcher);
			Playlist playlist = new Playlist(allFetcher, null);
			Status status = new Status(playlist, mock(DeviceList.class), mock(CollectionList.class), api, mock(FileSystemSync.class));

			compareRequestResponse(status, "/v3/current-collection", Method.PUT, null, "all", 204, "Not Changed", null, null);
			mockedAbstractFetcher.verify(() -> Fetcher.createFromSlug(api, "all"), times(0));
			mockedAbstractFetcher.verify(() -> Fetcher.createFromSlug(api, "relax"), times(0));
			assertEquals("all", playlist.getCurrentFetcherSlug());
			compareRequestResponse(status, "/v3/current-collection", Method.PUT, null, "relax", 204, "Changed", null, null);
			mockedAbstractFetcher.verify(() -> Fetcher.createFromSlug(api, "all"), times(0));
			mockedAbstractFetcher.verify(() -> Fetcher.createFromSlug(api, "relax"), times(1));
			assertEquals("relax", playlist.getCurrentFetcherSlug());
			compareRequestResponse(status, "/v3/current-collection", Method.PUT, null, "relax", 204, "Not Changed", null, null);
			mockedAbstractFetcher.verify(() -> Fetcher.createFromSlug(api, "all"), times(0));
			mockedAbstractFetcher.verify(() -> Fetcher.createFromSlug(api, "relax"), times(1));
			assertEquals("relax", playlist.getCurrentFetcherSlug());
		}
	}

	@Test
	void updateTrackTime() throws Exception {
		Playlist playlist = new Playlist(mock(Fetcher.class), null);
		Status status = new Status(playlist, mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		Track trackA = new Track(status.getMediaApi(), "http://example.com/track/2456", new HashMap<String, String>(Map.of("title", "Strawberry Fields", "artist", "The Beatles", "trackid", "2456")));
		playlist.queue(new Track[]{trackA});

		int hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/playlist/whatever/"+trackA.getUuid()+"/current-time", Method.PUT, null, "13.7", 204, "Changed", null, null);
		assertEquals(13.7f, trackA.getCurrentTime());

		compareRequestResponse(status, "/v3/playlist/whatever/unknown-uuid/current-time", Method.PUT, null, "24.6", 204, "Not Changed", null, null);
		assertEquals(13.7f, trackA.getCurrentTime());

		compareRequestResponse(status, "/v3/playlist/whatever/"+trackA.getUuid()+"/current-time", Method.PUT, null, "strawberry", 400, "Bad Request", "text/plain", "Invalid time given \"strawberry\".  Must be a number");
		assertEquals(13.7f, trackA.getCurrentTime());

		checkNotAllowed(status, "/v3/playlist/whatever/"+trackA.getUuid()+"/current-time", Method.POST, null, Arrays.asList(Method.PUT), true);

	}

	@Test
	void checkUnauthorisedResponse() throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/v3/current-collection");
		when(request.getMethod()).thenReturn(Method.PUT);
		when(request.isAuthorised()).thenReturn(false);
		Status status = new Status(mock(Playlist.class), mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).isAuthorised();
		verify(request).sendHeaders(401, "Unauthorized",  Map.of("Content-Type","text/plain","WWW-Authenticate","key"));
		verify(request).writeBody("Invalid API Key");
		verify(request).close();
	}

	@Test
	void corsPreflightWithoutAuth() throws Exception {
		Status status = new Status(mock(Playlist.class), mock(DeviceList.class), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		checkNotAllowed(status, "/v3/volume", Method.OPTIONS, null, Arrays.asList(Method.PUT), false);
	}
}