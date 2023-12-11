public abstract class Fetcher implements Runnable {
	protected Playlist playlist;
	public void setPlaylist(Playlist playlist) {
		if (this.playlist != null) throw new RuntimeException("Fetcher already has playlist associated");
		this.playlist = playlist;
	}
}