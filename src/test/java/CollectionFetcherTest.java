import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

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