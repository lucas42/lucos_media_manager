public abstract class Fetcher implements Runnable {
	protected Playlist playlist;
	protected Runnable postFetch;
	public void setPlaylist(Playlist playlist) {
		if (this.playlist != null) throw new RuntimeException("Fetcher already has playlist associated");
		this.playlist = playlist;
	}
	public void setPostFetchFunction(Runnable postFetch) {
		this.postFetch = postFetch;
	}
	protected void fetchComplete() {
		if (this.postFetch != null) this.postFetch.run();
	}
	abstract String getSlug();
	static Fetcher createFromSlug(MediaApi mediaApi, String slug) {
		if (slug.equals("all")) {
			return new RandomFetcher(mediaApi);
		} else {
			return new CollectionFetcher(mediaApi, slug);
		}
	}
}