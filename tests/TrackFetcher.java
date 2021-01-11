import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;


class TrackFetcherTest {

	@Test
	void fetchesTwentyTracks() {
		assertEquals(0, Manager.getPlaylistLength());

		// TODO: Mock out this URL, so it's not making requests from a live service
		TrackFetcher fetcher = new TrackFetcher("https://media-api.l42.eu/tracks/random");
		fetcher.run();

		// API returns 20 tracks - expect one to be added to "now" and rest as playlist
		assertEquals(19, Manager.getPlaylistLength());
	}

}