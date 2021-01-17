import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;


class RandomFetcherTest {

	@Test
	void fetchesTwentyTracks() {

		RandomFetcher fetcher = new RandomFetcher();
		Playlist playlist = mock(Playlist.class);
		fetcher.setPlaylist(playlist);
		fetcher.run();

		ArgumentCaptor<Track[]> tracksCaptor = ArgumentCaptor.forClass(Track[].class);
		verify(playlist).queue(tracksCaptor.capture());
		Track[] tracks = tracksCaptor.getValue();
		assertEquals(20, tracks.length);
	}

	@Test
	void tooManyPlaylists() {
		RandomFetcher fetcher = new RandomFetcher();
		Playlist playlistA = mock(Playlist.class);
		Playlist playlistB = mock(Playlist.class);
		fetcher.setPlaylist(playlistA);
		Exception exception = assertThrows(Exception.class, () -> {
			fetcher.setPlaylist(playlistB);
		});

		assertEquals("Fetcher already has playlist associated", exception.getMessage());
	}
}