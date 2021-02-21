import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
/**
 * Keeps track of which devices have an active connection to the server
 */
class ConnectionTracker {
	public Map<HttpRequest, Device> connections = new HashMap<HttpRequest, Device>();
	public void open(Device device, HttpRequest connection) {
		connections.put(connection, device);
	}
	public void close(HttpRequest connection) {
		connections.remove(connection);
	}
	public boolean isConnected(Device device) {
		Collection<Device> devices = connections.values();
		return devices.contains(device);
	}
}