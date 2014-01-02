
import java.util.*; 
import java.io.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
class Hosts {
	Map<String, String> hosts;
	public Hosts(String servicesDomain){

		try {
			String hostsurl = "http://"+servicesDomain+"/api/hosts";
			RemoteObject hostsRequest = new RemoteObject(hostsurl);

			Gson gson = new Gson();

			hosts = gson.fromJson(hostsRequest.getBody(), new TypeToken<Map<String, String>>() {}.getType());
		} catch (IOException e) {
			System.err.println(e);
			hosts = new HashMap<String, String>();
		}
	}
	public String get(String key) {
		return hosts.get(key);
	}
}