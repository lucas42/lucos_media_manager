class Status {
	private boolean isPlaying;
	private float volume;
	private Playlist playlist;
	private ConnectionTracker connections;
	private DeviceList deviceList;
	public Status(Playlist playlist, ConnectionTracker connections, DeviceList deviceList) {
		this.playlist = playlist;
		this.connections = connections;
		this.deviceList = deviceList;
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
			+ connections.hashCode()
			+ Boolean.hashCode(isPlaying)
			+ Float.hashCode(volume);
	}
}