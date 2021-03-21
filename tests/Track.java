import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
		assertEquals(null, noMetadataTrack.getMetadata("exturl"));
		final Track trackWithCustomExtUrl = new Track("https://example.com/track", new HashMap<String, String>(Map.of("exturl", "https://example.com/viewdetails")));
		assertEquals("https://example.com/viewdetails", trackWithCustomExtUrl.getMetadata("exturl"));
		final Track bbcProgrammeTrack = new Track("https://example.com/track", new HashMap<String, String>(Map.of("pid", "b006q2x0")));
		assertEquals("https://www.bbc.co.uk/programmes/b006q2x0", bbcProgrammeTrack.getMetadata("exturl"));
		final Track musicbrainzArtistTrack = new Track("https://example.com/track", new HashMap<String, String>(Map.of("mbid_artist", "db92a151-1ac2-438b-bc43-b82e149ddd50")));
		assertEquals("https://musicbrainz.org/artist/db92a151-1ac2-438b-bc43-b82e149ddd50", musicbrainzArtistTrack.getMetadata("exturl"));
	}
	@Test
	void editUrl() {
		final Track noMetadataTrack = new Track("https://example.com/track");
		assertEquals(null, noMetadataTrack.getMetadata("editurl"));
		final Track trackWithCustomEditUrl = new Track("https://example.com/track", new HashMap<String, String>(Map.of("editurl", "https://example.com/edit")));
		assertEquals("https://example.com/edit", trackWithCustomEditUrl.getMetadata("editurl"));
		final Track standardTrack = new Track("https://example.com/track", new HashMap<String, String>(Map.of("track_id", "42")));
		assertEquals("https://media-metadata.l42.eu/tracks/42", standardTrack.getMetadata("editurl"));
	}
	@Test
	void img() {
		final Track noMetadataTrack = new Track("https://example.com/track");
		assertEquals("https://staticmedia.l42.eu/music-pic.png", noMetadataTrack.getMetadata("img"));
		final Track musicbrainzArtistTrack = new Track("https://example.com/track", new HashMap<String, String>(Map.of("mbid_artist", "db92a151-1ac2-438b-bc43-b82e149ddd50")));
		assertEquals("https://staticmedia.l42.eu/artists/db92a151-1ac2-438b-bc43-b82e149ddd50.jpg", musicbrainzArtistTrack.getMetadata("img"));
		final Track trackWithCustomImg = new Track("https://example.com/track", new HashMap<String, String>(Map.of("img", "https://example.com/pic.png")));
		assertEquals("https://example.com/pic.png", trackWithCustomImg.getMetadata("img"));
	}
	@Test
	void thumb() {
		final Track noMetadataTrack = new Track("https://example.com/track");
		assertEquals("https://staticmedia.l42.eu/music-pic.png", noMetadataTrack.getMetadata("thumb"));
		final Track musicbrainzArtistTrack = new Track("https://example.com/track", new HashMap<String, String>(Map.of("mbid_artist", "db92a151-1ac2-438b-bc43-b82e149ddd50")));
		assertEquals("https://staticmedia.l42.eu/artists/db92a151-1ac2-438b-bc43-b82e149ddd50.jpg", musicbrainzArtistTrack.getMetadata("thumb"));
		final Track trackWithCustomThumb = new Track("https://example.com/track", new HashMap<String, String>(Map.of("thumb", "https://example.com/thumb.png")));
		assertEquals("https://example.com/thumb.png", trackWithCustomThumb.getMetadata("thumb"));
	}
	@Test
	void title() {
		final Track trackWithCustomTitle = new Track("https://example.com/track", new HashMap<String, String>(Map.of("title", "Track One")));
		assertEquals("Track One", trackWithCustomTitle.getMetadata("title"));
		final Track noMetadataTrack = new Track("https://example.com/track2");
		assertEquals("track2", noMetadataTrack.getMetadata("title"));
		final Track noMetadataTrackWithExtension = new Track("https://example.com/track3.ogg");
		assertEquals("track3", noMetadataTrackWithExtension.getMetadata("title"));
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
	@Test
	void hashCodes() {
		final Track trackA = new Track("https://example.com/track1");
		final Track trackB = new Track("https://example.com/track1");
		final Track trackC = new Track("https://example.com/track2");
		final Track trackWithoutUrlA = new Track(null);
		final Track trackWithoutUrlB = new Track(null);
		assertEquals(trackA.hashCode(), trackB.hashCode());
		assertEquals(trackA.hashCode(), trackA.hashCode());
		assertNotEquals(trackA.hashCode(), trackC.hashCode());
		assertNotEquals(trackWithoutUrlA.hashCode(), trackA.hashCode());
		assertEquals(trackWithoutUrlA.hashCode(), trackWithoutUrlB.hashCode());
	}

}