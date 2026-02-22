import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class DeviceTest {

	@Test
	void devicesDefaultNames() {
		Loganne mockLoganne = mock(Loganne.class);
		DeviceList deviceList = new DeviceList(mockLoganne);
		Device deviceA = deviceList.getDevice("uuid-A");
		deviceList.getDevice("uuid-B");
		Device deviceA2 = deviceList.getDevice("uuid-A");
		assertEquals(deviceA, deviceA2);
		deviceList.getDevice("uuid-C");
		Device[] devices = deviceList.getAllDevices();
		assertEquals(3, devices.length);
		assertEquals("Device 1", devices[0].getName());
		assertEquals("Device 2", devices[1].getName());
		assertEquals("Device 3", devices[2].getName());
	}

	@Test
	void customNames() {
		Loganne mockLoganne = mock(Loganne.class);
		DeviceList deviceList = new DeviceList(mockLoganne);
		deviceList.getDevice("uuid-A").setName("Aardvark");
		deviceList.getDevice("uuid-B");
		deviceList.getDevice("uuid-C").setName("Cardiff Castle");
		Device[] devices = deviceList.getAllDevices();
		assertEquals(3, devices.length);
		assertEquals("Aardvark", devices[0].getName());
		assertEquals("Device 2", devices[1].getName());
		assertEquals("Cardiff Castle", devices[2].getName());
	}

	@Test
	void trackCurrentDevice() {
		Loganne mockLoganne = mock(Loganne.class);
		DeviceList deviceList = new DeviceList(mockLoganne);

		// Should default to the first device being current
		deviceList.getDevice("uuid-A").setName("Aardvark");
		deviceList.getDevice("uuid-B");
		deviceList.getDevice("uuid-C").setName("Cardiff Castle");
		Device[] devices = deviceList.getAllDevices();
		assertEquals(3, devices.length);
		assertEquals(true, devices[0].isCurrent());
		assertEquals(false, devices[1].isCurrent());
		assertEquals(false, devices[2].isCurrent());
		verify(mockLoganne).post("deviceSwitch", "Playing music on first device connected");

		deviceList.setCurrent("uuid-C");
		devices = deviceList.getAllDevices();
		assertEquals(3, devices.length);
		// Only one device at a time should be current, so uuid-A is no longer current
		assertEquals(false, devices[0].isCurrent());
		assertEquals(false, devices[1].isCurrent());
		assertEquals(true, devices[2].isCurrent());
		verify(mockLoganne).post("deviceSwitch", "Moving music to play on Cardiff Castle");

		deviceList.setCurrent("uuid-B");
		devices = deviceList.getAllDevices();
		assertEquals(3, devices.length);
		assertEquals(false, devices[0].isCurrent());
		assertEquals(true, devices[1].isCurrent());
		assertEquals(false, devices[2].isCurrent());
		verify(mockLoganne).post("deviceSwitch", "Moving music to play on Device 2");

		// Setting the current device to be current again should have no effect
		deviceList.setCurrent("uuid-B");
		devices = deviceList.getAllDevices();
		assertEquals(3, devices.length);
		assertEquals(false, devices[0].isCurrent());
		assertEquals(true, devices[1].isCurrent());
		assertEquals(false, devices[2].isCurrent());
		verifyNoMoreInteractions(mockLoganne);

		// Setting current to a non-existant uuid should create a new device
		deviceList.setCurrent("uuid-D");
		devices = deviceList.getAllDevices();
		assertEquals(4, devices.length);
		assertEquals(false, devices[0].isCurrent());
		assertEquals(false, devices[1].isCurrent());
		assertEquals(false, devices[2].isCurrent());
		assertEquals(true, devices[3].isCurrent());
		verify(mockLoganne).post("deviceSwitch", "Moving music to play on Device 4");
	}

	@Test
	void hashcodesChange() {
		Loganne mockLoganne = mock(Loganne.class);
		DeviceList deviceList = new DeviceList(mockLoganne);
		int hashCode1 = deviceList.hashCode();
		deviceList.getDevice("uuid-A");
		int hashCode2 = deviceList.hashCode();
		assertNotEquals(hashCode2, hashCode1);
		deviceList.getDevice("uuid-A").setName("Rufus");
		int hashCode3 = deviceList.hashCode();
		assertNotEquals(hashCode3, hashCode2);
		deviceList.getDevice("uuid-B");
		int hashCode4 = deviceList.hashCode();
		assertNotEquals(hashCode4, hashCode3);
		deviceList.setCurrent("uuid-B");
		int hashCode5 = deviceList.hashCode();
		assertNotEquals(hashCode5, hashCode4);
	}
}