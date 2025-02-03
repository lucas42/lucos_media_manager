import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;


class CollectionFetcherTest {

	@Test
	void tooManyPlaylists() {
		CollectionFetcher fetcher = new CollectionFetcher(mock(MediaApi.class), "christmas");
		Playlist playlistA = mock(Playlist.class);
		Playlist playlistB = mock(Playlist.class);
		fetcher.setPlaylist(playlistA);
		Exception exception = assertThrows(Exception.class, () -> {
			fetcher.setPlaylist(playlistB);
		});

		assertEquals("Fetcher already has playlist associated", exception.getMessage());
	}
}