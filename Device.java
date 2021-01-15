import java.util.Map;
import java.util.HashMap;

class Device {
	private static Map<String, Device> all = new HashMap<String, Device>();
	private String uuid;
	private String name;
	private Device(String uuid) {
		this.uuid = uuid;
		all.put(uuid, this);
		this.name = "Device "+all.size();
	}
	public String getName() {
		return name;
	}
	public static Device getInstance(String uuid) {
		Device instance = all.get(uuid);
		if (instance != null) return instance;
		return new Device(uuid);
	}
	public static Device[] getAll() {
		return all.values().toArray(new Device[0]);
	}
}