import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

class FallbackControllerTest {

	@Test
	void redirectsOldPlayerPagesToCurrentPlayer() throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/");
		Status status = new Status(mock(Playlist.class), mock(DeviceList.class), mock(CollectionList.class),
				mock(MediaApi.class), mock(FileSystemSync.class));
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).redirect("https://seinn.l42.eu/");
		verify(request).close();
	}

	@Test
	void returnsGoneForRetiredEndpoints() throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/devices/current");
		Status status = new Status(mock(Playlist.class), mock(DeviceList.class), mock(CollectionList.class),
				mock(MediaApi.class), mock(FileSystemSync.class));
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(410, "Gone", "text/plain");
		verify(request).writeBody("Endpoint no longer supported");
		verify(request).close();
	}

	@Test
	void retunsNotFound() throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/never-been-used");
		Status status = new Status(mock(Playlist.class), mock(DeviceList.class), mock(CollectionList.class),
				mock(MediaApi.class), mock(FileSystemSync.class));
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(404, "Not Found", "text/plain");
		verify(request).writeBody("File Not Found");
		verify(request).close();
	}

}