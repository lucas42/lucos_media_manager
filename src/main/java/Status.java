import java.util.Map;
import java.util.HashMap;

class Status {
	private boolean isPlaying;
	private float volume;
	private Playlist playlist;
	private DeviceList deviceList;
	private CollectionList collectionList;
	private MediaApi mediaApi;
	private FileSystemSync fsSync;

	/**
	 * Lock used by the wait/notify mechanism so poll threads can efficiently wait
	 * for state changes without busy-polling.
	 */
	private final Object changeLock = new Object();
	public Status(Playlist playlist, DeviceList deviceList, CollectionList collectionList, MediaApi mediaApi, FileSystemSync fsSync) {
		this.playlist = playlist;
		this.deviceList = deviceList;
		this.collectionList = collectionList;
		this.mediaApi = mediaApi;
		this.fsSync = fsSync;
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
		if (isPlaying && !wasPlaying) {
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
		return (this.hashCode() != oldhashcode);
	}

	/**
	 * Wakes any threads waiting in {@link #waitForChange}.
	 * Should be called whenever state changes so that long-poll threads respond immediately.
	 */
	public void notifyChange() {
		synchronized (changeLock) {
			changeLock.notifyAll();
		}
	}

	/**
	 * Waits until state changes from the given hashcode, or the timeout elapses.
	 * Uses a condition variable so the calling thread doesn't busy-poll.
	 * The hashcode is re-checked inside the lock to avoid missing a notification
	 * that arrives between the caller's outer check and entering this method.
	 */
	public void waitForChange(int currentHashcode, long timeoutMs) throws InterruptedException {
		synchronized (changeLock) {
			if (this.hashCode() == currentHashcode) {
				changeLock.wait(timeoutMs);
			}
		}
	}
	public Map<String, Object> getSummary() {
		Map<String, Object> summary = new HashMap<String, Object>();
		summary.put("tracks", playlist);
		summary.put("volume", this.getVolume());
		summary.put("isPlaying", this.getPlaying());
		summary.put("devices", deviceList.getActiveDevices());
		summary.put("collections", collectionList.getAllCollections(getCurrentFetcherSlug()));
		summary.put("currentCollectionSlug", getCurrentFetcherSlug());
		summary.put("hashcode", this.hashCode());
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
		return playlist.getCurrentFetcherSlug();
	}
	public MediaApi getMediaApi() {
		return mediaApi;
	}
	public void syncToFileSystem() {
		fsSync.writeStatus(this);
	}
}