import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.math.BigInteger;

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

		final CountDownLatch initialFetch = new CountDownLatch(1);
		doAnswer(invocation -> {
			initialFetch.countDown();
			return null;
		}).when(fetcher).run();

		for (int ii=0; ii<15; ii++) {
			tracks[ii] = new Track(mock(MediaApi.class), "https://example.com/track/"+ii);
		}

		Playlist playlist = new Playlist(fetcher, loganne);
		verify(fetcher).setPlaylist(playlist);

		playlist.topupTracks();
		verify(loganne).post("fetchTracks", "Fetching more tracks to add to the current playlist");

		boolean ended = initialFetch.await(10, TimeUnit.SECONDS);
		assertTrue(ended, "Fetcher thread should return normally, rather than timeout");
		verify(fetcher).run();
		playlist.queue(tracks);

		assertEquals(15, playlist.getLength());

		// Cycle through the first 5 tracks (out of 15)
		// These shouldn't trigger a fetch
		for (int ii=0; ii<5; ii++) {
			playlist.skipTrack();
			verifyNoMoreInteractions(fetcher);
			verifyNoMoreInteractions(loganne);
		}
		assertEquals(10, playlist.getLength());

		final CountDownLatch fetching = new CountDownLatch(1);
		final CountDownLatch callingNext = new CountDownLatch(5);
		doAnswer(invocation -> {
			callingNext.await(); // Wait until skipTrack() has been called a bunch of times
			fetching.countDown();
			return null;
		}).when(fetcher).run();

		// When calling skipTrack() results in the number of tracks being below 10
		// the fetcher should be called
		playlist.skipTrack();
		assertEquals(9, playlist.getLength());
		verify(loganne, times(2)).post("fetchTracks", "Fetching more tracks to add to the current playlist");

		// Shouldn't trigger another fetch when a previous one is still in flight
		for (int ii=0; ii<5; ii++) {
			playlist.skipTrack();
			verifyNoMoreInteractions(loganne);
			callingNext.countDown();
		}

		ended = fetching.await(10, TimeUnit.SECONDS);
		assertTrue(ended, "Fetcher thread should return normally, rather than timeout");
		verify(fetcher, times(2)).run();
		verifyNoMoreInteractions(loganne);
		assertEquals(4, playlist.getLength());
	}

	@Test
	// Under normal operation, a playlist shouldn't be empty for long
	// But in case skipTrack() is called when it is empty, verify no exception is thrown
	void nextOnEmptyPlaylist() {
		Playlist playlist = new Playlist(mock(Fetcher.class), null);
		playlist.skipTrack();
	}

	@Test
	void manipulatePlaylist() {
		Track trackA = new Track(mock(MediaApi.class), "https://example.com/trackA");
		Track trackB = new Track(mock(MediaApi.class), "https://example.com/trackB");
		Track trackC = new Track(mock(MediaApi.class), "https://example.com/trackC");
		Track trackD = new Track(mock(MediaApi.class), "https://example.com/trackD");
		Playlist playlist = new Playlist(mock(Fetcher.class), null);

			assertEquals(0, playlist.getLength());
			assertEquals(null, playlist.getCurrentTrack());
			assertEquals(null, playlist.getNextTrack());
			int oldHashcode = playlist.hashCode();
			int newHashcode = playlist.hashCode();
			assertEquals(oldHashcode, newHashcode);

		playlist.queueNext(trackA);
			assertEquals(1, playlist.getLength());
			assertEquals(trackA, playlist.getCurrentTrack());
			assertEquals(null, playlist.getNextTrack());

			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

		playlist.queueNow(trackB);
			assertEquals(2, playlist.getLength());
			assertEquals(trackB, playlist.getCurrentTrack());
			assertEquals(trackA, playlist.getNextTrack());

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

		playlist.queueNext(trackC);
			assertEquals(3, playlist.getLength());
			assertEquals(trackB, playlist.getCurrentTrack());
			assertEquals(trackC, playlist.getNextTrack());

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

		playlist.queueEnd(trackD);
			assertEquals(4, playlist.getLength());
			assertEquals(trackB, playlist.getCurrentTrack());
			assertEquals(trackC, playlist.getNextTrack());

			Track[] tracks = playlist.getTracks().toArray(new Track[4]);
			assertEquals(trackB, tracks[0]);
			assertEquals(trackC, tracks[1]);
			assertEquals(trackA, tracks[2]);
			assertEquals(trackD, tracks[3]);

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

		playlist.skipTrack(trackC);
			assertEquals(3, playlist.getLength());
			assertEquals(trackB, playlist.getCurrentTrack());
			assertEquals(trackA, playlist.getNextTrack());

			tracks = playlist.getTracks().toArray(new Track[3]);
			assertEquals(trackB, tracks[0]);
			assertEquals(trackA, tracks[1]);
			assertEquals(trackD, tracks[2]);

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

		playlist.completeTrack(trackB);
			assertEquals(2, playlist.getLength());
			assertEquals(trackA, playlist.getCurrentTrack());
			assertEquals(trackD, playlist.getNextTrack());

			tracks = playlist.getTracks().toArray(new Track[2]);
			assertEquals(trackA, tracks[0]);
			assertEquals(trackD, tracks[1]);

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

		playlist.flagTrackAsError(trackA, "Http status 404 returned");
			assertEquals(1, playlist.getLength());
			assertEquals(trackD, playlist.getCurrentTrack());
			assertEquals(null, playlist.getNextTrack());

			tracks = playlist.getTracks().toArray(new Track[1]);
			assertEquals(trackD, tracks[0]);

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

	}

	@Test
	void manipulatePlaylistByUuid() {
		Track trackA = new Track(mock(MediaApi.class), "https://example.com/trackA");
		Track trackB = new Track(mock(MediaApi.class), "https://example.com/trackB");
		Track trackC = new Track(mock(MediaApi.class), "https://example.com/trackC");
		Track trackD = new Track(mock(MediaApi.class), "https://example.com/trackD");
		String uuidA = trackA.getUuid();
		String uuidB = trackB.getUuid();
		String uuidC = trackC.getUuid();
		String uuidD = trackD.getUuid();
		Playlist playlist = new Playlist(mock(Fetcher.class), null);
		playlist.queue(new Track[]{trackB, trackC, trackA, trackD});
			int oldHashcode = playlist.hashCode();
			int newHashcode = playlist.hashCode();
			assertEquals(oldHashcode, newHashcode);

		assertTrue(playlist.skipTrack(uuidC));
			assertEquals(3, playlist.getLength());
			assertEquals(trackB, playlist.getCurrentTrack());
			assertEquals(trackA, playlist.getNextTrack());

			Track[] tracks = playlist.getTracks().toArray(new Track[3]);
			assertEquals(trackB, tracks[0]);
			assertEquals(trackA, tracks[1]);
			assertEquals(trackD, tracks[2]);

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

		assertTrue(playlist.completeTrack(uuidB));
			assertEquals(2, playlist.getLength());
			assertEquals(trackA, playlist.getCurrentTrack());
			assertEquals(trackD, playlist.getNextTrack());

			tracks = playlist.getTracks().toArray(new Track[2]);
			assertEquals(trackA, tracks[0]);
			assertEquals(trackD, tracks[1]);

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

		assertTrue(playlist.flagTrackAsError(uuidA, "Http status 404 returned"));
			assertEquals(1, playlist.getLength());
			assertEquals(trackD, playlist.getCurrentTrack());
			assertEquals(null, playlist.getNextTrack());

			tracks = playlist.getTracks().toArray(new Track[1]);
			assertEquals(trackD, tracks[0]);

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertNotEquals(oldHashcode, newHashcode);

		assertFalse(playlist.skipTrack("01321f3e-87ca-4417-8183-0dc7d83e379f"));  // Uuid which doesn't match any in the list should have no impact
			assertEquals(1, playlist.getLength());
			assertEquals(trackD, playlist.getCurrentTrack());
			assertEquals(null, playlist.getNextTrack());

			tracks = playlist.getTracks().toArray(new Track[1]);
			assertEquals(trackD, tracks[0]);

			oldHashcode = newHashcode;
			newHashcode = playlist.hashCode();
			assertEquals(oldHashcode, newHashcode);

	}

	@Test
	void trackTimes() {
		boolean returnVal;
		Track trackA = new Track(mock(MediaApi.class), "https://example.com/trackA");
		Track trackB = new Track(mock(MediaApi.class), "https://example.com/trackB");
		Track trackC = new Track(mock(MediaApi.class), "https://example.com/trackC");
		Track trackD = new Track(mock(MediaApi.class), "https://example.com/trackD");
		Playlist playlist = new Playlist(mock(Fetcher.class), null);
		playlist.queueEnd(trackA);
		playlist.queueEnd(trackB);
		playlist.queueEnd(trackC);
		playlist.queueEnd(trackD);

		returnVal = playlist.setTrackTime(new Track(mock(MediaApi.class), "https://example.com/trackC"), 13.7f, new BigInteger("946684800"));
		assertTrue(returnVal);
		assertEquals(trackC.getCurrentTime(), 13.7f);

		returnVal = playlist.setTrackTime(new Track(mock(MediaApi.class), "https://example.com/trackB"), 69f, new BigInteger("946684800"));
		assertTrue(returnVal);
		assertEquals(trackB.getCurrentTime(), 69f);

		returnVal = playlist.setTrackTime(new Track(mock(MediaApi.class), "https://example.com/trackF"), 66.6f, new BigInteger("946684800"));
		assertFalse(returnVal);
		assertEquals(trackA.getCurrentTime(), 0f);
		assertEquals(trackD.getCurrentTime(), 0f);
	}

	@Test
	void trackTimesByUuid() {
		boolean returnVal;
		Track trackA = new Track(mock(MediaApi.class), "https://example.com/trackA");
		Track trackB = new Track(mock(MediaApi.class), "https://example.com/trackB");
		Track trackC = new Track(mock(MediaApi.class), "https://example.com/trackC");
		Track trackD = new Track(mock(MediaApi.class), "https://example.com/trackD");
		Playlist playlist = new Playlist(mock(Fetcher.class), null);
		playlist.queueEnd(trackA);
		playlist.queueEnd(trackB);
		playlist.queueEnd(trackC);
		playlist.queueEnd(trackD);

		returnVal = playlist.setTrackTimeByUuid(trackC.getUuid(), 13.7f);
		assertTrue(returnVal);
		assertEquals(trackC.getCurrentTime(), 13.7f);

		returnVal = playlist.setTrackTimeByUuid(trackB.getUuid(), 69f);
		assertTrue(returnVal);
		assertEquals(trackB.getCurrentTime(), 69f);

		returnVal = playlist.setTrackTimeByUuid("unknown-uuid", 66.6f);
		assertFalse(returnVal);
		assertEquals(trackA.getCurrentTime(), 0f);
		assertEquals(trackD.getCurrentTime(), 0f);
	}
}