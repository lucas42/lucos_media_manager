import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.HashMap;

class TrackTest {

	@Test
	void keepsUrl() {
		final Track track = new Track("https://example.com/track");
		assertEquals("https://example.com/track", track.getUrl());
	}
	@Test
	void extUrl() {
		final Track noMetadataTrack = new Track("https://example.com/track");
		assertEquals(null, noMetadataTrack.getExtUrl());
		final Track trackWithCustomExtUrl = new Track("https://example.com/track", new HashMap<String, String>(Map.of("exturl", "https://example.com/viewdetails")));
		assertEquals("https://example.com/viewdetails", trackWithCustomExtUrl.getExtUrl());
		final Track bbcProgrammeTrack = new Track("https://example.com/track", new HashMap<String, String>(Map.of("pid", "b006q2x0")));
		assertEquals("https://www.bbc.co.uk/programmes/b006q2x0", bbcProgrammeTrack.getExtUrl());
		final Track musicbrainzArtistTrack = new Track("https://example.com/track", new HashMap<String, String>(Map.of("mbid_artist", "db92a151-1ac2-438b-bc43-b82e149ddd50")));
		assertEquals("https://musicbrainz.org/artist/db92a151-1ac2-438b-bc43-b82e149ddd50", musicbrainzArtistTrack.getExtUrl());
	}
	@Test
	void editUrl() {
		final Track noMetadataTrack = new Track("https://example.com/track");
		assertEquals(null, noMetadataTrack.getEditUrl());
		final Track trackWithCustomEditUrl = new Track("https://example.com/track", new HashMap<String, String>(Map.of("editurl", "https://example.com/edit")));
		assertEquals("https://example.com/edit", trackWithCustomEditUrl.getEditUrl());
		final Track standardTrack = new Track("https://example.com/track", new HashMap<String, String>(Map.of("track_id", "42")));
		assertEquals("null?id=42", standardTrack.getEditUrl());
	}
	@Test
	void equality() {
		final Track trackA = new Track("https://example.com/track1");
		final Track trackB = new Track("https://example.com/track1");
		final Track trackC = new Track("https://example.com/track2");
		final Track trackWithoutUrlA = new Track(null);
		final Track trackWithoutUrlB = new Track(null);
		assertNotEquals(trackA, null);
		assertEquals(trackA, trackB);
		assertEquals(trackA, trackA);
		assertNotEquals(trackA, trackC);
		assertNotEquals(trackA, new HashMap<String, String>());
		assertNotEquals(trackWithoutUrlA, trackA);
		assertEquals(trackWithoutUrlA, trackWithoutUrlB);
	}

}