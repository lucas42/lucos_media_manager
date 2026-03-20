import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MediaApiTest {

	@Test
	void connectTimeoutIsSet() {
		assertTrue(MediaApi.CONNECT_TIMEOUT_MS > 0, "Connect timeout must be a positive value");
		assertTrue(MediaApi.CONNECT_TIMEOUT_MS <= 10_000, "Connect timeout should be at most 10 seconds");
	}

	@Test
	void readTimeoutIsSet() {
		assertTrue(MediaApi.READ_TIMEOUT_MS > 0, "Read timeout must be a positive value");
		assertTrue(MediaApi.READ_TIMEOUT_MS <= 60_000, "Read timeout should be at most 60 seconds");
	}

}
