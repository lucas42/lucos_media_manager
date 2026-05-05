import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

class InfoControllerTest {

	@Test
	void infoReturnedOK() throws Exception {
		Playlist playlist = mock(Playlist.class);
		when(playlist.getLength()).thenReturn(7);
		when(playlist.isFetcherRunning()).thenReturn(false);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class),
				mock(FileSystemSync.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/_info");
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(200, "OK", "application/json");
		verify(request).writeBody(
				"{\"system\":\"lucos_media_manager\",\"title\":\"Media Manager\",\"checks\":{\"empty-queue\":{\"techDetail\":\"Queue has any tracks, or is being actively repopulated\",\"ok\":true,\"failThreshold\":2},\"queue\":{\"techDetail\":\"Queue has at least 5 tracks\",\"ok\":true,\"failThreshold\":3}},\"metrics\":{\"queue-length\":{\"techDetail\":\"Number of tracks in queue\",\"value\":7}},\"ci\":{\"circle\":\"gh/lucas42/lucos_media_manager\"}}");
		verify(request).close();
	}

	@Test
	void playlistLowOnTracks() throws Exception {
		Playlist playlist = mock(Playlist.class);
		when(playlist.getLength()).thenReturn(2);
		when(playlist.isFetcherRunning()).thenReturn(false);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class),
				mock(FileSystemSync.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/_info");
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(200, "OK", "application/json");
		verify(request).writeBody(
				"{\"system\":\"lucos_media_manager\",\"title\":\"Media Manager\",\"checks\":{\"empty-queue\":{\"techDetail\":\"Queue has any tracks, or is being actively repopulated\",\"ok\":true,\"failThreshold\":2},\"queue\":{\"techDetail\":\"Queue has at least 5 tracks\",\"ok\":false,\"failThreshold\":3}},\"metrics\":{\"queue-length\":{\"techDetail\":\"Number of tracks in queue\",\"value\":2}},\"ci\":{\"circle\":\"gh/lucas42/lucos_media_manager\"}}");
		verify(request).close();
	}

	@Test
	void PlaylistEmpty() throws Exception {
		Playlist playlist = mock(Playlist.class);
		when(playlist.getLength()).thenReturn(0);
		when(playlist.isFetcherRunning()).thenReturn(false);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class),
				mock(FileSystemSync.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/_info");
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(200, "OK", "application/json");
		verify(request).writeBody(
				"{\"system\":\"lucos_media_manager\",\"title\":\"Media Manager\",\"checks\":{\"empty-queue\":{\"techDetail\":\"Queue has any tracks, or is being actively repopulated\",\"ok\":false,\"failThreshold\":2},\"queue\":{\"techDetail\":\"Queue has at least 5 tracks\",\"ok\":false,\"failThreshold\":3}},\"metrics\":{\"queue-length\":{\"techDetail\":\"Number of tracks in queue\",\"value\":0}},\"ci\":{\"circle\":\"gh/lucas42/lucos_media_manager\"}}");
		verify(request).close();
	}

	@Test
	void PlaylistEmptyButFetcherRunning() throws Exception {
		// Queue is empty but the fetcher thread is alive — this is the normal state
		// during a collection switch while tracks are being fetched. The empty-queue
		// check should report ok:true so monitoring doesn't false-alarm.
		Playlist playlist = mock(Playlist.class);
		when(playlist.getLength()).thenReturn(0);
		when(playlist.isFetcherRunning()).thenReturn(true);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class),
				mock(FileSystemSync.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/_info");
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(200, "OK", "application/json");
		verify(request).writeBody(
				"{\"system\":\"lucos_media_manager\",\"title\":\"Media Manager\",\"checks\":{\"empty-queue\":{\"techDetail\":\"Queue has any tracks, or is being actively repopulated\",\"ok\":true,\"failThreshold\":2},\"queue\":{\"techDetail\":\"Queue has at least 5 tracks\",\"ok\":false,\"failThreshold\":3}},\"metrics\":{\"queue-length\":{\"techDetail\":\"Number of tracks in queue\",\"value\":0}},\"ci\":{\"circle\":\"gh/lucas42/lucos_media_manager\"}}");
		verify(request).close();
	}

}
