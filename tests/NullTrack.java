import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import java.util.HashMap;

class NullTrackTest {

	@Test
	void allNullTracksAreEqual() {
		NullTrack nullTrackA = new NullTrack();
		NullTrack nullTrackB = new NullTrack("https://example.com/nulltrack");
		NullTrack nullTrackC = new NullTrack("https://example.com/nullytrack", new HashMap<String, String>());
		Track realTrack = new Track("https://example.com/track");
		assertEquals(nullTrackA, nullTrackA);
		assertEquals(nullTrackA, nullTrackB);
		assertEquals(nullTrackB, nullTrackC);
		assertEquals(nullTrackC, nullTrackA);
		assertNotEquals(nullTrackA, realTrack);
		assertNotEquals(realTrack, nullTrackB);
		assertNotEquals(nullTrackC, null);
	}
	@Test
	void hashCodeIsAlwaysZero() {
		NullTrack nullTrackA = new NullTrack();
		assertEquals(0, nullTrackA.hashCode());
	}

}