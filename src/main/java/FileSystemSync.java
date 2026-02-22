import java.io.*;
import java.net.MalformedURLException;
import com.google.gson.Gson;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public final class FileSystemSync {
	public static final String FILE_NAME = "manager-status.json";
	private String filePath;

	public FileSystemSync(String directoryPath) {
		filePath = directoryPath + "/" + FILE_NAME;
	}

	/**
	 * Reads the state from the file system, if found
	 * Otherwise, returns a blank status object
	 */
	public Status readStatus(Loganne loganne, MediaApi mediaApi) throws MalformedURLException {

		Playlist playlist = new Playlist(new RandomFetcher(mediaApi), loganne);
		DeviceList deviceList = new DeviceList(loganne);
		CollectionList collectionList = new CollectionList(mediaApi);
		Status status = new Status(playlist, deviceList, collectionList, mediaApi, this);
		try {
			FileReader reader = new FileReader(filePath);
			Gson gson = new Gson();
			@SuppressWarnings("unchecked")
			Map<String, Object> map = gson.fromJson(reader, HashMap.class);
			status.setVolume(((Double) map.get("volume")).floatValue());
			status.setPlaying((boolean) map.get("isPlaying"));
			@SuppressWarnings("unchecked")
			ArrayList<Map<String, Object>> devices = (ArrayList<Map<String, Object>>) map.get("devices");
			for (Map<String, Object> device : devices) {
				if ((boolean) device.get("isDefaultName")) {
					device.put("name", null);
				}
				deviceList.updateDevice((String) device.get("uuid"), (String) device.get("name"));
				if ((boolean) device.get("isCurrent")) {
					deviceList.setCurrent((String) device.get("uuid"));
				}
			}
			Fetcher fetcher = Fetcher.createFromSlug(mediaApi, (String) map.get("currentCollectionSlug"));
			playlist.setFetcher(fetcher);

			@SuppressWarnings("unchecked")
			ArrayList<Map<String, Object>> tracks = (ArrayList<Map<String, Object>>) map.get("tracks");
			for (Map<String, Object> trackData : tracks) {

				// Only copy the url from the stored data, then refresh it to ensure latest
				// metadata for track is used
				Track track = new Track(mediaApi, (String) trackData.get("url"));
				track.refreshMetadata();
				playlist.queueEnd(track);
			}
		} catch (Exception e) {
			System.out.println("WARNING: Can't read status from file.  Using blank status.");
			e.printStackTrace(System.err);
		}

		playlist.topupTracks();
		return status;
	}

	public void writeStatus(Status status) {
		try {
			FileWriter writer = new FileWriter(filePath);
			Gson gson = CustomGson.get(status);
			gson.toJson(status.getSummary(), writer);
			writer.close();
		} catch (IOException e) {
			System.err.println("ERROR: Can't write status to filesystem");
			e.printStackTrace(System.err);
		}
	}
}