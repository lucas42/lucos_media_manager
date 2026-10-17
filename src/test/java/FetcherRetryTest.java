import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class FetcherRetryTest {

	private static Track track() {
		return new Track(mock(MediaApi.class), "http://example.com/track/1", new java.util.HashMap<String, String>(java.util.Map.of("trackid", "1")));
	}

	private static MediaApiResult result(Track... tracks) {
		MediaApiResult r = new MediaApiResult();
		r.tracks = tracks;
		return r;
	}

	@Test
	void retriesWithBackoffThenQueuesTracks() throws Exception {
		MediaApi api = mock(MediaApi.class);
		Track t = track();
		when(api.fetchTracks(anyString())).thenThrow(new RuntimeException("timeout")).thenReturn(result()).thenReturn(result(t));
		Playlist playlist = new Playlist(new RandomFetcher(api), null);
		CollectionFetcher fetcher = new CollectionFetcher(api, "x", mock(CollectionList.class));
		playlist.setFetcher(fetcher);
		List<Long> waits = new ArrayList<>();
		fetcher.setSleeper(waits::add);
		Runnable post = mock(Runnable.class);
		fetcher.setPostFetchFunction(post);

		fetcher.run();

		assertEquals(List.of(5000L, 10000L), waits);
		assertEquals(1, playlist.getLength());
		assertFalse(fetcher.hasFailedAttempt());
		verify(post, times(1)).run();
	}

	@Test
	void backoffCapsAtSixtySeconds() throws Exception {
		MediaApi api = mock(MediaApi.class);
		when(api.fetchTracks(anyString())).thenThrow(new RuntimeException("timeout"), new RuntimeException("timeout"),
				new RuntimeException("timeout"), new RuntimeException("timeout"), new RuntimeException("timeout"),
				new RuntimeException("timeout"), new RuntimeException("timeout")).thenReturn(result(track()));
		RandomFetcher fetcher = new RandomFetcher(api);
		Playlist playlist = new Playlist(fetcher, null);
		List<Long> waits = new ArrayList<>();
		fetcher.setSleeper(waits::add);

		fetcher.run();

		assertEquals(List.of(5000L, 10000L, 20000L, 40000L, 60000L, 60000L, 60000L), waits);
		assertEquals(1, playlist.getLength());
	}

	@Test
	void failedAttemptIsFlaggedWhileRetrying() throws Exception {
		MediaApi api = mock(MediaApi.class);
		when(api.fetchTracks(anyString())).thenThrow(new RuntimeException("timeout"));
		RandomFetcher fetcher = new RandomFetcher(api);
		Playlist playlist = new Playlist(fetcher, null);
		boolean[] flaggedDuringWait = { false };
		fetcher.setSleeper(ms -> {
			flaggedDuringWait[0] = fetcher.hasFailedAttempt();
			playlist.setFetcher(new RandomFetcher(api)); // abandon, so the loop ends
		});

		fetcher.run();

		assertTrue(flaggedDuringWait[0]);
	}

	@Test
	void abandonedFetcherQueuesNothing() throws Exception {
		MediaApi api = mock(MediaApi.class);
		when(api.fetchTracks(anyString())).thenThrow(new RuntimeException("timeout")).thenReturn(result(track()));
		RandomFetcher fetcher = new RandomFetcher(api);
		Playlist playlist = new Playlist(fetcher, null);
		fetcher.setSleeper(ms -> playlist.setFetcher(new RandomFetcher(api)));

		fetcher.run();

		assertEquals(0, playlist.getLength());
		verify(api, times(1)).fetchTracks(anyString());
	}

	@Test
	void fetchInFlightDuringSwitchQueuesNothing() throws Exception {
		MediaApi api = mock(MediaApi.class);
		RandomFetcher fetcher = new RandomFetcher(api);
		Playlist playlist = new Playlist(fetcher, null);
		when(api.fetchTracks(anyString())).thenAnswer(inv -> {
			playlist.setFetcher(new RandomFetcher(api)); // collection switches mid-fetch
			return result(track());
		});

		fetcher.run();

		assertEquals(0, playlist.getLength());
	}

	@Test
	void activelyRepopulatingOnlyUntilFirstFailure() throws Exception {
		MediaApi api = mock(MediaApi.class);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch waiting = new CountDownLatch(1);
		when(api.fetchTracks(anyString())).thenThrow(new RuntimeException("timeout")).thenReturn(result(track()));
		RandomFetcher fetcher = new RandomFetcher(api);
		Playlist playlist = new Playlist(fetcher, null);
		fetcher.setSleeper(ms -> {
			waiting.countDown();
			try { release.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
		});
		assertFalse(playlist.isActivelyRepopulating());

		playlist.topupTracks();
		assertTrue(waiting.await(5, TimeUnit.SECONDS));
		assertTrue(playlist.isFetcherRunning());
		assertFalse(playlist.isActivelyRepopulating());

		release.countDown();
		for (int i = 0; i < 100 && playlist.isFetcherRunning(); i++) Thread.sleep(50);
		assertEquals(1, playlist.getLength());
	}
}
