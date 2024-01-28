import java.util.Map;
import java.util.HashMap;

class Status {
	private boolean isPlaying;
	private float volume;
	private Playlist playlist;
	private DeviceList deviceList;
	private CollectionList collectionList;
	public Status(Playlist playlist, DeviceList deviceList, CollectionList collectionList) {
		this.playlist = playlist;
		this.deviceList = deviceList;
		this.collectionList = collectionList;
		volume = (float)0.5;
		isPlaying = true;
	}
	public boolean getPlaying() {
		return this.isPlaying;
	}
	public void setPlaying(boolean isPlaying) {
		boolean wasPlaying = getPlaying();
		this.isPlaying = isPlaying;
		
		// If unpausing, then update timeset in the current track, so nothing is missed
		if (isPlaying && !wasPlaying && playlist != null) {
			Track now = playlist.getCurrentTrack();
			if (now != null) now.timeSetNow();
		}
	}
	public float getVolume() {
		return this.volume;
	}
	public void setVolume(float volume) {
		if (volume > 1) volume = 1;
		if (volume < 0) volume = 0;
		this.volume = volume;
	}
	public int hashCode() {
		return playlist.hashCode()
			+ deviceList.hashCode()
			+ collectionList.hashCode()
			+ Boolean.hashCode(isPlaying)
			+ Float.hashCode(volume);
	}
	public boolean summaryHasChanged(int oldhashcode) {
		if (playlist == null) return true;
		return (this.hashCode() != oldhashcode);
	}
	public Map<String, Object> getSummary() {
		Map<String, Object> summary = new HashMap<String, Object>();
		summary.put("tracks", playlist);
		summary.put("volume", this.getVolume());
		summary.put("isPlaying", this.getPlaying());
		summary.put("devices", deviceList.getAllDevices());
		summary.put("collections", collectionList.getAllCollections(getCurrentFetcherSlug()));
		if (playlist != null) summary.put("hashcode", this.hashCode());
		return summary;
	}
	public Playlist getPlaylist() {
		return playlist;
	}
	public DeviceList getDeviceList() {
		return deviceList;
	}
	public CollectionList getCollectionList() {
		return collectionList;
	}
	private String getCurrentFetcherSlug() {
		if (playlist == null) return null;
		return playlist.getCurrentFetcherSlug();
	}
}