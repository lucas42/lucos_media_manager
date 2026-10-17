import java.util.function.LongConsumer;

public abstract class Fetcher implements Runnable {
	// Waits between attempts; the last value is reused for every later attempt
	private static final long[] BACKOFF_SECONDS = { 5, 10, 20, 40, 60 };

	protected Playlist playlist;
	protected Runnable postFetch;
	private volatile boolean hasFailedAttempt = false;
	private LongConsumer sleeper = Fetcher::sleepMs;

	public void setPlaylist(Playlist playlist) {
		if (this.playlist != null) throw new RuntimeException("Fetcher already has playlist associated");
		this.playlist = playlist;
	}
	public void setPostFetchFunction(Runnable postFetch) {
		this.postFetch = postFetch;
	}
	void setSleeper(LongConsumer sleeper) {
		this.sleeper = sleeper;
	}
	protected void fetchComplete() {
		if (this.postFetch != null) this.postFetch.run();
	}

	/**
	 * True once an attempt has failed (or returned no tracks) and the fetcher is still retrying
	 */
	public boolean hasFailedAttempt() {
		return hasFailedAttempt;
	}

	/**
	 * Fetches a batch of tracks from the media API, retrying until it gets some
	 * or the playlist switches to a different fetcher.
	 */
	@Override
	public void run() {
		for (int attempt = 1; playlist.isCurrentFetcher(this); attempt++) {
			try {
				Track[] tracks = fetchBatch();
				if (tracks == null || tracks.length == 0) throw new Exception("Fetch returned no tracks");
				playlist.queue(tracks);
				hasFailedAttempt = false;
				System.err.println("DEBUG: New tracks added to playlist from " + getSlug());
				this.fetchComplete();
				return;
			} catch (Exception e) {
				hasFailedAttempt = true;
				long waitSeconds = BACKOFF_SECONDS[Math.min(attempt, BACKOFF_SECONDS.length) - 1];
				System.err.println("ERROR: Can't fetch new tracks (attempt " + attempt + ", retrying in " + waitSeconds + "s)");
				e.printStackTrace(System.err);
				try {
					sleeper.accept(waitSeconds * 1000);
				} catch (RuntimeException interrupted) {
					return;
				}
			}
		}
	}

	private static void sleepMs(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	abstract Track[] fetchBatch() throws Exception;
	abstract String getSlug();
	abstract String getName();
	static Fetcher createFromSlug(MediaApi mediaApi, CollectionList collectionList, String slug) {
		if (slug.equals("all")) return new RandomFetcher(mediaApi);
		else return new CollectionFetcher(mediaApi, slug, collectionList);
	}
}
