import java.util.Map;
import java.util.HashMap;

/**
 * Represents a group of Devices
 * Only one device at a time should be marked as the "current" one
 */
class DeviceList {
	private Map<String, Device> devices = new HashMap<String, Device>();
	private ConnectionTracker connections = new ConnectionTracker();
	private Loganne loganne;

	public DeviceList(Loganne loganne) {
		this.loganne = loganne;
	}
	/**
	 * Returns the device with the given uuid, if it exists
	 * Otherwise, creates a device with the uuid and returns it
	 */
	public Device getDevice(String uuid) {
		Device instance = devices.get(uuid);
		if (instance != null) return instance;
		instance = new Device(this, uuid);
		devices.put(uuid, instance);
		return instance;
	}

	/**
	 * Returns an array of all devices
	 */
	public Device[] getAllDevices() {
		return devices.values().toArray(new Device[0]);
	}

	/**
	 * Sets one device to be "current" (ie which should play music)
	 * Only one device should be current at a time
	 * If a device with the given uuid doesn't exist, it is created
	 */
	public void setCurrent(String uuid) {
		Device newCurrent = getDevice(uuid);

		// Don't bother doing anything if this device is already current
		if (newCurrent.isCurrent()) return;
		for (Device device : devices.values()) {
			device.isCurrent = false;
		}
		newCurrent.isCurrent = true;
		if (loganne != null) loganne.post("deviceSwitch", "Moving music to play on "+newCurrent.getName());
	}

	/**
	 * Returns the number of devices currently in the list
	 */
	public int size() {
		return devices.size();
	}

	@Override
	public int hashCode() {
		int sumHashCodes = 0;
		for (Device device : devices.values()) {
			sumHashCodes += device.hashCode(connections);
		}
		return sumHashCodes;
	}
	public Device openConnection(HttpRequest request) {
		String device_uuid = request.removeParam("device");
		if (device_uuid == null) return null;
		Device device = this.getDevice(device_uuid);
		connections.open(device, request);
		return device;
	}
	public void closeConnection(HttpRequest request) {
		connections.close(request);
	}
	public void updateDevice(String uuid, String name) {
		Device device = this.getDevice(uuid);
		if (name != null) {
			device.setName(name);
		}
	}
	public boolean isConnected(Device device) {
		return connections.isConnected(device);
	}
}