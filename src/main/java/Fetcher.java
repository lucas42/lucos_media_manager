public abstract class Fetcher implements Runnable {
	protected Playlist playlist;
	public void setPlaylist(Playlist playlist) {
		if (this.playlist != null) throw new RuntimeException("Fetcher already has playlist associated");
		this.playlist = playlist;
	}
	abstract String getSlug();
	static Fetcher createFromSlug(String slug) {
		if (slug.equals("all")) {
			return new RandomFetcher();
		} else {
			return new CollectionFetcher(slug);
		}
	}
}