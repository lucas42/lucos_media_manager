import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import java.util.Map;
import java.util.Collection;
import java.util.Arrays;
import java.util.HashMap;
import java.io.IOException;

class ControllerV3Test {
	void compareRequestResponse(Status status, String path, Method method, String requestBody, int responseStatus, String responseString, String contentType, String responseBody) throws IOException {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn(path);
		when(request.getMethod()).thenReturn(method);
		when(request.getData()).thenReturn(requestBody);
		ControllerV3 controller = new ControllerV3(status, request);
		controller.processRequest();
		if (contentType != null) verify(request).sendHeaders(responseStatus, responseString, contentType);
		else verify(request).sendHeaders(responseStatus, responseString);
		if (responseBody != null) verify(request).writeBody(responseBody);
		verify(request).close();
	}
	void checkNotAllowed(Status status, String path, Method method, Collection<Method> allowedMethods) throws IOException {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn(path);
		when(request.getMethod()).thenReturn(method);
		ControllerV3 controller = new ControllerV3(status, request);
		controller.processRequest();

		verify(request).notAllowed(allowedMethods);
	}

	@Test
	void unknownPathReturns404() throws IOException {
		Status status = new Status(null, new DeviceList(null), mock(CollectionList.class));
		compareRequestResponse(status, "/v3/unknown", Method.GET, null, 404, "Not Found", "text/plain", "File Not Found");
	}

	@Test
	void playingPause() throws IOException {
		Status status = new Status(null, new DeviceList(null), mock(CollectionList.class));
		status.setPlaying(true);
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "False", 204, "Changed", null, null);
		assertFalse(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "False", 204, "Changed", null, null);
		assertFalse(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "True", 204, "Changed", null, null);
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "True", 204, "Changed", null, null);
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "Unknown", 400, "Not Changed", "text/plain", "Unknown value \"Unknown\""); // Test unknown value doesn't change anything
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "fAlSe", 204, "Changed", null, null); // Test case insensitive
		assertFalse(status.getPlaying());
		checkNotAllowed(status, "/v3/is-playing", Method.POST, Arrays.asList(Method.PUT));
		assertFalse(status.getPlaying());
	}

	@Test
	void volume() throws IOException {
		Status status = new Status(null, new DeviceList(null), mock(CollectionList.class));

		// The following values should update the volume
		compareRequestResponse(status, "/v3/volume", Method.PUT, "1.0", 204, "Changed", null, null);
		assertEquals(1, status.getVolume(), 0);
		compareRequestResponse(status, "/v3/volume", Method.PUT, "0", 204, "Changed", null, null);
		assertEquals(0, status.getVolume(), 0);
		compareRequestResponse(status, "/v3/volume", Method.PUT, "0.7", 204, "Changed", null, null);
		assertEquals(0.7, status.getVolume(), 0.0002);

		// The following values should all fail, and not update the volume
		compareRequestResponse(status, "/v3/volume", Method.PUT, null, 400, "Not Changed", "text/plain", "Request body must be set to value for volume");
		compareRequestResponse(status, "/v3/volume", Method.PUT, "string", 400, "Not Changed", "text/plain", "Volume must be a number");
		compareRequestResponse(status, "/v3/volume", Method.PUT, "NaN", 400, "Not Changed", "text/plain", "Volume must not be greater than 1.0");
		compareRequestResponse(status, "/v3/volume", Method.PUT, "-1", 400, "Not Changed", "text/plain", "Volume must not be less than 0.0");
		compareRequestResponse(status, "/v3/volume", Method.PUT, "1.3", 400, "Not Changed", "text/plain", "Volume must not be greater than 1.0");
		checkNotAllowed(status, "/v3/volume", Method.POST, Arrays.asList(Method.PUT));
		assertEquals(0.7, status.getVolume(), 0.0002);
	}

	@Test
	void setDeviceName() throws IOException {
		Status status = new Status(null, new DeviceList(null), mock(CollectionList.class));

		// Create a device if none exists, and set its name
		compareRequestResponse(status, "/v3/device-names/47d9cba3-fd9f-445d-b984-072e4f75732c", Method.PUT, "Home Laptop", 204, "Changed", null, null);
		assertEquals("Home Laptop", status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").getName()); // Device created and given a name
		compareRequestResponse(status, "/v3/device-names/a7441bd5-65a1-4357-bc96-c0ece53def07", Method.PUT, "Phone", 204, "Changed", null, null);
		assertEquals("Phone", status.getDeviceList().getDevice("a7441bd5-65a1-4357-bc96-c0ece53def07").getName()); // Device created and given a name
		assertEquals("Home Laptop", status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").getName()); // Existing device unaffected
		compareRequestResponse(status, "/v3/device-names/47d9cba3-fd9f-445d-b984-072e4f75732c", Method.PUT, "Personal Laptop", 204, "Changed", null, null);
		assertEquals("Personal Laptop", status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").getName()); // Existing device's name updated
		assertEquals("Phone", status.getDeviceList().getDevice("a7441bd5-65a1-4357-bc96-c0ece53def07").getName()); // Existing divec unnafected

		checkNotAllowed(status, "/v3/device-names/47d9cba3-fd9f-445d-b984-072e4f75732c", Method.POST, Arrays.asList(Method.PUT));
	}

	@Test
	void setCurrentDevice() throws IOException {
		Loganne loganne = mock(Loganne.class);
		Status status = new Status(null, new DeviceList(loganne), mock(CollectionList.class));

		// Create a device if none exists, and set its name
		compareRequestResponse(status, "/v3/current-device", Method.PUT, "47d9cba3-fd9f-445d-b984-072e4f75732c", 204, "Changed", null, null);
		assertTrue(status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").isCurrent()); // Device created and set as current one
		verify(loganne).post("deviceSwitch", "Playing music on first device connected");
		compareRequestResponse(status, "/v3/current-device", Method.PUT, "a7441bd5-65a1-4357-bc96-c0ece53def07", 204, "Changed", null, null);
		assertTrue(status.getDeviceList().getDevice("a7441bd5-65a1-4357-bc96-c0ece53def07").isCurrent()); // Device created and set as current one
		assertFalse(status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").isCurrent()); // Existing device marked as not current
		verify(loganne).post("deviceSwitch", "Moving music to play on Device 2");
		compareRequestResponse(status, "/v3/current-device", Method.PUT, "47d9cba3-fd9f-445d-b984-072e4f75732c", 204, "Changed", null, null);
		assertTrue(status.getDeviceList().getDevice("47d9cba3-fd9f-445d-b984-072e4f75732c").isCurrent()); // Existing device set as current one
		assertFalse(status.getDeviceList().getDevice("a7441bd5-65a1-4357-bc96-c0ece53def07").isCurrent()); // Existing device marked as not current
		verify(loganne).post("deviceSwitch", "Moving music to play on Device 1");

		checkNotAllowed(status, "/v3/current-device", Method.POST, Arrays.asList(Method.PUT));

	}

	@Test
	void trackComplete() throws IOException {
		Fetcher fetcher = mock(RandomFetcher.class);
		Loganne loganne = mock(Loganne.class);

		// Create playlist with 4 tracks, 2 of which are the same track
		Playlist playlist = new Playlist(fetcher, loganne);
		playlist.queueNext(new Track("http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347"))));
		playlist.queueNext(new Track("http://example.com/track/8532", new HashMap<String, String>(Map.of("title", "Good as Gold", "artist", "Beautiful South", "trackid", "8532"))));
		playlist.queueNext(new Track("http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347"))));
		playlist.queueNext(new Track("http://example.com/track/8533", new HashMap<String, String>(Map.of("title", "Old Red Eyes Is Back", "artist", "Beautiful South", "trackid", "8533"))));

		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class));
		int hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/track-complete", Method.POST, "http://example.com/track/1347", 204, "Changed", null, null);  // Removes the first instance of the track

		assertEquals(3, playlist.getLength());
		assertNotEquals("Stairway To Heaven", playlist.getCurrentTrack().getMetadata("title"));
		assertEquals("Stairway To Heaven", playlist.getNextTrack().getMetadata("title"));
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/track-complete", Method.POST, "http://example.com/track/1347", 204, "Changed", null, null);  // Removes the second instance of the track
		assertEquals(2, playlist.getLength());
		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

		compareRequestResponse(status, "/v3/track-complete", Method.POST, "http://example.com/track/1347", 204, "Not Changed", null, null);  // No instances of track remain - no-op
		assertEquals(2, playlist.getLength());
		assertEquals(hashCode, status.hashCode());

		compareRequestResponse(status, "/v3/track-complete", Method.POST, null, 400, "Bad Request", "text/plain", "Missing track url from request body");  // No instances of track remain - no-op
		assertEquals(2, playlist.getLength());
		assertEquals(hashCode, status.hashCode());

		checkNotAllowed(status, "/v3/track-complete", Method.PUT, Arrays.asList(Method.POST));

	}
}