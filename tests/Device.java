import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import java.net.* ;
import java.io.* ;

class DeviceTest {

	@Test
	void devicesDefaultNames() {
		Device deviceA = Device.getInstance("uuid-A");
		Device deviceB = Device.getInstance("uuid-B");
		Device deviceA2 = Device.getInstance("uuid-A");
		assertEquals(deviceA, deviceA2);
		Device deviceC = Device.getInstance("uuid-C");
		Device[] devices = Device.getAll();
		assertEquals(3, devices.length);
		assertEquals("Device 1", devices[0].getName());
		assertEquals("Device 2", devices[1].getName());
		assertEquals("Device 3", devices[2].getName());
	}
}