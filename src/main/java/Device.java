
/**
 * Represents devices which play the media
 * Devices themselves should generate a uniquie uuid once each
 * They also have a name, which is configurable
 * One device at a time should be marked as the "current" one
 */
class Device {
	private String uuid;
	private String name;
	@SuppressWarnings("unused")
	private boolean isDefaultName;
	protected boolean isCurrent = false;

	/**
	 * Don't construct devices to be constructed directly - use DeviceList.getDevice
	 * instead
	 * Uses a count of existing devices to default to naming devices numerically
	 */
	protected Device(DeviceList list, String uuid) {
		this.uuid = uuid;
		this.name = "Device " + (list.size() + 1);
		this.isDefaultName = true;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (this.name == name)
			return;
		this.name = name;
		this.isDefaultName = false;
	}

	public boolean isCurrent() {
		return isCurrent;
	}

	public int hashCode(ConnectionTracker connections) {
		int currentCode = isCurrent ? uuid.hashCode() : 0;
		int connectedCode = connections.isConnected(this) ? uuid.hashCode() * 2 : 0;
		// No need to check isDefaultName as that only changes when name changes
		return uuid.hashCode() + name.hashCode() + currentCode + connectedCode;
	}
}