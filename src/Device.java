import java.util.Map;
import java.util.HashMap;

/**
 * Represents devices which play the media
 * Devices themselves should generate a uniquie uuid once each
 * They also have a name, which is configurable
 * One device at a time should be marked as the "current" one
 */
class Device {
	private String uuid;
	private String name;
	protected boolean isCurrent = false;

	/**
	 * Don't construct devices to be constructed directly - use DeviceList.getDevice instead
	 * Uses a count of existing devices to default to naming devices numerically
	 */
	protected Device(DeviceList list, String uuid) {
		this.uuid = uuid;
		this.name = "Device "+(list.size()+1);

		// If this is the only device, it should be marked as current
		if (list.size() == 0) this.isCurrent = true;
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
	public int hashCode() {
		int currentCode = isCurrent ? uuid.hashCode() : 0;
		return uuid.hashCode() + name.hashCode() + currentCode;
	}
}