import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

class RobotsControllerTest {

	@Test
	void disallowAll() throws Exception {
		Status status = new Status(null, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class), mock(FileSystemSync.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/robots.txt");
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(200, "OK", "text/plain");
		verify(request).writeBody("User-agent: *");
		verify(request).writeBody("Disallow: /");
		verify(request).close();
	}
}