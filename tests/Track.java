import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrackTest {

    private final Track track = new Track("https://example.com/track");

    @Test
    void keepsUrl() {
        assertEquals("https://example.com/track", track.getUrl());
    }

}