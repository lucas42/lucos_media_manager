import java.util.Map;
import java.util.HashMap;

/**
 * Represents devices which play the media
 * Devices themselves should generate a uniquie uuid once each
 * They also have a name, which is configurable
 * One device at a time should be marked as the "current" one
 */
class Device {
	private static Map<String, Device> all = new HashMap<String, Device>();
	private String uuid;
	private String name;
	private boolean isCurrent = false;

	/**
	 * Don't allow devices to be constructed directly - use Device.getInstance instead
	 * This prevents multiple devices existing with the same uuid (ie assume it's the same device)
	 * Defaults to naming devices numerically
	 */
	private Device(String uuid) {
		this.uuid = uuid;
		all.put(uuid, this);
		this.name = "Device "+all.size();

		// If this is the only device, it should be marked as current
		if (all.size() == 1) this.isCurrent = true;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isCurrent() {
		return isCurrent;
	}

	/**
	 * Returns the device with the given uuid, if it exists
	 * Otherwise, creates a device with the uuid and returns it
	 */
	public static Device getInstance(String uuid) {
		Device instance = all.get(uuid);
		if (instance != null) return instance;
		return new Device(uuid);
	}

	/**
	 * Returns an array of all devices
	 */
	public static Device[] getAll() {
		return all.values().toArray(new Device[0]);
	}

	/**
	 * Sets one device to be "current" (ie which should play music)
	 * Only one device should be current at a time
	 * If a device with the given uuid doesn't exist, it is created
	 */
	public static void setCurrent(String uuid) {
		for (Device device : all.values()) {
			device.isCurrent = false;
		}
		getInstance(uuid).isCurrent = true;
	}
}