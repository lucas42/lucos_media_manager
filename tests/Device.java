import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import java.net.* ;
import java.io.* ;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeviceTest {

	@Test
	@Order(1)
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
	@Test
	@Order(2)
	void customNames() {
		Device.getInstance("uuid-A").setName("Aardvark");
		Device.getInstance("uuid-C").setName("Cardiff Castle");
		Device[] devices = Device.getAll();
		assertEquals(3, devices.length);
		assertEquals("Aardvark", devices[0].getName());
		assertEquals("Device 2", devices[1].getName());
		assertEquals("Cardiff Castle", devices[2].getName());
	}

	@Test
	@Order(3) // Use the devices set up in the names tests
	void trackCurrentDevice() {
		Device[] devices = Device.getAll();
		assertEquals(3, devices.length);
		assertEquals(false, devices[0].isCurrent());
		assertEquals(false, devices[1].isCurrent());
		assertEquals(false, devices[2].isCurrent());

		Device.setCurrent("uuid-C");
		devices = Device.getAll();
		assertEquals(3, devices.length);
		assertEquals(false, devices[0].isCurrent());
		assertEquals(false, devices[1].isCurrent());
		assertEquals(true, devices[2].isCurrent());

		Device.setCurrent("uuid-A");
		devices = Device.getAll();
		assertEquals(3, devices.length);
		assertEquals(true, devices[0].isCurrent());
		assertEquals(false, devices[1].isCurrent());
		assertEquals(false, devices[2].isCurrent());

		// Setting current to a non-existant uuid should create a new device
		Device.setCurrent("uuid-D");
		devices = Device.getAll();
		assertEquals(4, devices.length);
		assertEquals(false, devices[0].isCurrent());
		assertEquals(false, devices[1].isCurrent());
		assertEquals(false, devices[2].isCurrent());
		assertEquals(true, devices[3].isCurrent());
	}
}