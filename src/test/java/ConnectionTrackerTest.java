import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ConnectionTrackerTest {

	@Test
	void simpleOpenCloseConnection() {
		ConnectionTracker tracker = new ConnectionTracker();
		HttpRequest request1 = mock(HttpRequest.class);
		Device deviceA = mock(Device.class);
		assertFalse(tracker.isConnected(deviceA));
		tracker.open(deviceA, request1);
		assertTrue(tracker.isConnected(deviceA));
		tracker.close(request1);
		assertFalse(tracker.isConnected(deviceA));
	}

	@Test
	void parallelConnections() {
		ConnectionTracker tracker = new ConnectionTracker();
		HttpRequest request1 = mock(HttpRequest.class);
		Device deviceA = mock(Device.class);
		assertFalse(tracker.isConnected(deviceA));
		tracker.open(deviceA, request1);
		assertTrue(tracker.isConnected(deviceA));
		HttpRequest request2 = mock(HttpRequest.class);
		tracker.open(deviceA, request2);
		assertTrue(tracker.isConnected(deviceA));
		tracker.close(request1);
		assertTrue(tracker.isConnected(deviceA));
		tracker.close(request2);
		assertFalse(tracker.isConnected(deviceA));
	}
	@Test
	void multipleDevices() {
		ConnectionTracker tracker = new ConnectionTracker();
		Device deviceA = mock(Device.class);
		Device deviceB = mock(Device.class);
		HttpRequest request1 = mock(HttpRequest.class);
		assertFalse(tracker.isConnected(deviceA));
		assertFalse(tracker.isConnected(deviceB));
		tracker.open(deviceA, request1);
		assertTrue(tracker.isConnected(deviceA));
		assertFalse(tracker.isConnected(deviceB));
		HttpRequest request2 = mock(HttpRequest.class);
		tracker.open(deviceB, request2);
		assertTrue(tracker.isConnected(deviceA));
		assertTrue(tracker.isConnected(deviceB));
		HttpRequest request3 = mock(HttpRequest.class);
		tracker.open(deviceA, request3);
		assertTrue(tracker.isConnected(deviceA));
		assertTrue(tracker.isConnected(deviceB));
		tracker.close(request2);
		assertTrue(tracker.isConnected(deviceA));
		assertFalse(tracker.isConnected(deviceB));
		tracker.close(request3);
		assertTrue(tracker.isConnected(deviceA));
		assertFalse(tracker.isConnected(deviceB));
		tracker.close(request1);
		assertFalse(tracker.isConnected(deviceA));
		assertFalse(tracker.isConnected(deviceB));
	}
}