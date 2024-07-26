import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
		compareRequestResponse(status, "/v3/unknown", Method.GET, null, 404, "Not Found", "text/plain", "File Not Found\n");
	}

	@Test
	void playingPause() throws Exception {
		Status status = new Status(null, new DeviceList(null), mock(CollectionList.class));
		status.setPlaying(true);
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "False", 204, "No Content", null, null);
		assertFalse(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "False", 204, "No Content", null, null);
		assertFalse(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "True", 204, "No Content", null, null);
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "True", 204, "No Content", null, null);
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "Unknown", 400, "Bad Request", "text/plain", "Unknown value \"Unknown\"\n"); // Test unknown value doesn't change anything
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "/v3/is-playing", Method.PUT, "fAlSe", 204, "No Content", null, null); // Test case insensitive
		assertFalse(status.getPlaying());
		checkNotAllowed(status, "/v3/is-playing", Method.POST, Arrays.asList(Method.PUT));
		assertFalse(status.getPlaying());
	}
}