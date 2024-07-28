import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import java.util.Map;
import java.util.Collection;
import java.util.Arrays;

class ControllerV3Test {
	void compareRequestResponse(Status status, String path, Method method, String requestBody, int responseStatus, String responseString, String contentType, String responseBody) throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn(path);
		when(request.getMethod()).thenReturn(method);
		when(request.getData()).thenReturn(requestBody);
		Controller controller = new ControllerV3(status, request);
		controller.processRequest();
		if (contentType != null) verify(request).sendHeaders(responseStatus, responseString, contentType);
		else verify(request).sendHeaders(responseStatus, responseString);
		if (responseBody != null) verify(request).writeBody(responseBody);
		verify(request).close();
	}
	void checkNotAllowed(Status status, String path, Method method, Collection<Method> allowedMethods) throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn(path);
		when(request.getMethod()).thenReturn(method);
		Controller controller = new ControllerV3(status, request);
		controller.processRequest();

		verify(request).notAllowed(allowedMethods);
	}

	@Test
	void unknownPathReturns404() throws Exception {
		Status status = new Status(null, new DeviceList(null), mock(CollectionList.class));
		compareRequestResponse(status, "/v3/unknown", Method.GET, null, 404, "Not Found", "text/plain", "File Not Found");
	}

	@Test
	void playingPause() throws Exception {
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
	void volume() throws Exception {
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
	void setDeviceName() throws Exception {
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

	}
}