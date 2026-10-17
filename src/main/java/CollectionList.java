import java.util.Arrays;

/**
 * Represents the list of available collections in the media library
 */
class CollectionList {
	private volatile MediaCollection[] collections;
	private MediaApi api;
	private transient Thread retryThread;
	static long RETRY_INTERVAL_MS = 30_000;

	public CollectionList(MediaApi api) {
		this.api = api;
		startFetchThread();
	}

	// Fetches the list in the background, retrying until it succeeds, so construction never blocks on the media API
	private void startFetchThread() {
		retryThread = new Thread(() -> {
			while (true) {
				refreshList();
				if (collections != null) return;
				System.err.println("INFO: Retrying collection list fetch after failure");
				try {
					Thread.sleep(RETRY_INTERVAL_MS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		});
		retryThread.setDaemon(true);
		retryThread.start();
	}

	// Pulls down a fresh list of collections from the media library
	// Returns a boolean of whether the fetch was successful
	public boolean refreshList() {
		try {
			MediaCollection[] fetched = api.fetchCollections("/v3/collections");
			this.collections = Arrays.stream(fetched).filter(collection -> collection.isPlayable).toArray(MediaCollection[]::new);
			System.err.println("DEBUG: New collection list fetched from media api");
			return true;
		} catch (Exception e) {
			System.err.println("ERROR: Can't fetch collections.");
			e.printStackTrace(System.err);
			return false;
		}
	}

	public MediaCollection[] getAllCollections(String currentSlug) {
		MediaCollection[] loaded = this.collections;
		if (loaded == null) return new MediaCollection[0];
		for (MediaCollection collection: loaded) {
			collection.isCurrent = (collection.slug.equals(currentSlug));
		}
		return loaded;
	}

	/**
	 * Returns the human-readable name of the collection with the given slug,
	 * or the slug itself if not found (e.g. collection list not yet loaded).
	 */
	public String getNameForSlug(String slug) {
		if (collections == null) return slug;
		for (MediaCollection collection : collections) {
			if (collection.slug.equals(slug)) return collection.name;
		}
		return slug;
	}


	@Override
	public int hashCode() {
		if (collections == null) return 0;
		return collections.hashCode();
	}
}

class MediaCollection {
	String slug;
	String name;
	String icon;
	int totalTracks;
	int totalPages;
	boolean isPlayable;
	boolean isCurrent;
}