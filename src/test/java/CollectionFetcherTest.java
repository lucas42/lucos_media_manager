import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;


class CollectionFetcherTest {

	@Test
	void fetchesTwentyTracks() {

		CollectionFetcher fetcher = new CollectionFetcher("halloween");
		Playlist playlist = mock(Playlist.class);
		fetcher.setPlaylist(playlist);
		fetcher.run();

		ArgumentCaptor<Track[]> tracksCaptor = ArgumentCaptor.forClass(Track[].class);
		verify(playlist).queue(tracksCaptor.capture());
		Track[] tracks = tracksCaptor.getValue();
		assertEquals(20, tracks.length);

		for (Track track: tracks) {
			assertNotNull(track.getMetadata());
		}
	}

	@Test
	void tooManyPlaylists() {
		CollectionFetcher fetcher = new CollectionFetcher("christmas");
		Playlist playlistA = mock(Playlist.class);
		Playlist playlistB = mock(Playlist.class);
		fetcher.setPlaylist(playlistA);
		Exception exception = assertThrows(Exception.class, () -> {
			fetcher.setPlaylist(playlistB);
		});

		assertEquals("Fetcher already has playlist associated", exception.getMessage());
	}
}