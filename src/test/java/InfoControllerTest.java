import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

import java.net.*;
import java.io.*;
import java.util.*;
class InfoControllerTest {

	@Test
	void infoReturnedOK() throws Exception {
		Playlist playlist = mock(Playlist.class);
		when(playlist.getLength()).thenReturn(7);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/_info");
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(200, "OK", "application/json");
		verify(request).writeBody("{\"system\":\"lucos_media_manager\",\"checks\":{\"empty-queue\":{\"techDetail\":\"Queue has any tracks\",\"ok\":true},\"queue\":{\"techDetail\":\"Queue has at least 5 tracks\",\"ok\":true}},\"ci\":{\"circle\":\"gh/lucas42/lucos_media_manager\"},\"metrics\":{\"queue-length\":{\"techDetail\":\"Number of tracks in queue\",\"value\":7}}}");
		verify(request).close();
	}
	@Test
	void playlistLowOnTracks() throws Exception {
		Playlist playlist = mock(Playlist.class);
		when(playlist.getLength()).thenReturn(2);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/_info");
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(200, "OK", "application/json");
		verify(request).writeBody("{\"system\":\"lucos_media_manager\",\"checks\":{\"empty-queue\":{\"techDetail\":\"Queue has any tracks\",\"ok\":true},\"queue\":{\"techDetail\":\"Queue has at least 5 tracks\",\"ok\":false}},\"ci\":{\"circle\":\"gh/lucas42/lucos_media_manager\"},\"metrics\":{\"queue-length\":{\"techDetail\":\"Number of tracks in queue\",\"value\":2}}}");
		verify(request).close();
	}
	@Test
	void PlaylistEmpty() throws Exception {
		Playlist playlist = mock(Playlist.class);
		when(playlist.getLength()).thenReturn(0);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn("/_info");
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).sendHeaders(200, "OK", "application/json");
		verify(request).writeBody("{\"system\":\"lucos_media_manager\",\"checks\":{\"empty-queue\":{\"techDetail\":\"Queue has any tracks\",\"ok\":false},\"queue\":{\"techDetail\":\"Queue has at least 5 tracks\",\"ok\":false}},\"ci\":{\"circle\":\"gh/lucas42/lucos_media_manager\"},\"metrics\":{\"queue-length\":{\"techDetail\":\"Number of tracks in queue\",\"value\":0}}}");
		verify(request).close();
	}

}