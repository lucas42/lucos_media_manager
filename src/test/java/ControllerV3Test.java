import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ControllerV3Test {

	@Test
	void unknownPathReturns404() throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/v3/unknown");
		Status status = new Status(null, new DeviceList(null), mock(CollectionList.class));
		Controller controller = new ControllerV3(status, request);
		controller.processRequest();
		verify(request).sendHeaders(404, "Not Found", "text/plain");
		verify(request).writeBody("File Not Found\n");
		verify(request).close();
	}
}