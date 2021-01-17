import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class PlaylistTest {

	@Test
	void creatingPlaylistCausesTopup() {
		Fetcher fetcher = mock(Fetcher.class);
		CountDownLatch finished = new CountDownLatch(1);

		doAnswer(invocation -> {
			finished.countDown();
			return null;
		}).when(fetcher).run();

		Playlist playlist = new Playlist(fetcher, null);

		verify(fetcher).setPlaylist(playlist);
	}
	@Test
	void topups() throws InterruptedException {
		Fetcher fetcher = mock(RandomFetcher.class);
		Loganne loganne = mock(Loganne.class);
		Track[] tracks = new Track[15];
		for (int ii=0; ii<15; ii++) {
			tracks[ii] = new Track("https://example.com/track/"+ii);
		}

		final CountDownLatch initialFetch = new CountDownLatch(1);
		doAnswer(invocation -> {
			initialFetch.countDown();
			return null;
		}).when(fetcher).run();

		Playlist playlist = new Playlist(fetcher, loganne);
		verify(fetcher).setPlaylist(playlist);
		verify(loganne).post("fetchTracks", "Fetching more tracks to add to the current playlist");

		boolean ended = initialFetch.await(10, TimeUnit.SECONDS);
		assertTrue(ended, "Fetcher thread should return normally, rather than timeout");
		verify(fetcher).run();
		playlist.queue(tracks);

		// TODO: check there's now 15 tracks in playlist?

		// Cycle through the first 5 tracks (out of 15)
		// These shouldn't trigger a fetch
		for (int ii=0; ii<5; ii++) {
			playlist.next();
			verifyNoMoreInteractions(fetcher);
			verifyNoMoreInteractions(loganne);
		}

		final CountDownLatch fetching = new CountDownLatch(1);
		final CountDownLatch callingNext = new CountDownLatch(5);
		doAnswer(invocation -> {
			callingNext.await(); // Wait until next() has been called a bunch of times
			fetching.countDown();
			return null;
		}).when(fetcher).run();

		// When calling next() results in the number of tracks being below 10
		// the fetcher should be called
		playlist.next();
		verify(loganne, times(2)).post("fetchTracks", "Fetching more tracks to add to the current playlist");

		// Shouldn't trigger another fetch when a previous one is still in flight
		for (int ii=0; ii<5; ii++) {
			playlist.next();
			verifyNoMoreInteractions(loganne);
			callingNext.countDown();
		}

		ended = fetching.await(10, TimeUnit.SECONDS);
		assertTrue(ended, "Fetcher thread should return normally, rather than timeout");
		verify(fetcher, times(2)).run();
		verifyNoMoreInteractions(loganne);
	}

	@Test
	// Under normal operation, a playlist shouldn't be empty for long
	// But in case next() is called when it is empty, verify no exception is thrown
	void nextOnEmptyPlaylist() {
		Playlist playlist = new Playlist(mock(Fetcher.class), null);
		playlist.next();
	}
}