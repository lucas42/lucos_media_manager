import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.*;
import com.google.gson.Gson;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public final class FileSystemSync {
	public static final String FILE_NAME = "manager-status.json";
	private String filePath;
	private final List<Track> restoredTracks = new ArrayList<>();

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
			Fetcher fetcher = Fetcher.createFromSlug(mediaApi, collectionList, (String) map.get("currentCollectionSlug"));
			playlist.setFetcher(fetcher);

			@SuppressWarnings("unchecked")
			ArrayList<Map<String, Object>> tracks = (ArrayList<Map<String, Object>>) map.get("tracks");
			for (Map<String, Object> trackData : tracks) {

				// Queue from the stored data; refreshRestoredTracksAsync() fetches the latest metadata later
				Track track = new Track(mediaApi, (String) trackData.get("url"), storedMetadata(trackData.get("metadata")));
				restoredTracks.add(track);
				playlist.queueEnd(track);
			}
		} catch (Exception e) {
			System.out.println("WARNING: Can't read status from file.  Using blank status.");
			e.printStackTrace(System.err);
		}

		playlist.topupTracks();
		return status;
	}

	private static Map<String, String> storedMetadata(Object stored) {
		Map<String, String> metadata = new HashMap<>();
		if (stored instanceof Map) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) stored).entrySet()) {
				if (entry.getKey() != null && entry.getValue() != null) {
					metadata.put(entry.getKey().toString(), entry.getValue().toString());
				}
			}
		}
		return metadata;
	}

	/**
	 * Refreshes the metadata of the tracks restored by readStatus() on a background thread,
	 * so a slow media API never delays startup. Failures are logged and skipped.
	 */
	public Thread refreshRestoredTracksAsync() {
		final List<Track> toRefresh = new ArrayList<>(restoredTracks);
		restoredTracks.clear();
		Thread thread = new Thread(() -> {
			for (Track track : toRefresh) {
				try {
					track.refreshMetadata();
				} catch (Exception e) {
					System.err.println("WARNING: Can't refresh metadata for restored track " + track.getUrl());
					e.printStackTrace(System.err);
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	public void writeStatus(Status status) {
		Path target = Paths.get(filePath);
		Path tmp = null;
		try {
			tmp = Files.createTempFile(target.getParent(), FILE_NAME, ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				CustomGson.get(status).toJson(status.getSummary(), writer);
			}
			Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.err.println("ERROR: Can't write status to filesystem");
			e.printStackTrace(System.err);
			if (tmp != null) {
				try {
					Files.deleteIfExists(tmp);
				} catch (IOException ignored) {}
			}
		}
	}
}