import java.util.Arrays;

/**
 * Represents the list of available collections in the media library
 */
class CollectionList {
	private MediaCollection[] collections;
	private MediaApi api;
	private transient Thread retryThread;
	static long RETRY_INTERVAL_MS = 30_000;

	public CollectionList(MediaApi api) {
		this.api = api;
		if (!this.refreshList()) {
			startRetryThread();
		}
	}

	private void startRetryThread() {
		if (retryThread != null && retryThread.isAlive()) return;
		retryThread = new Thread(() -> {
			while (collections == null) {
				try {
					Thread.sleep(RETRY_INTERVAL_MS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				refreshList();
			}
		});
		retryThread.setDaemon(true);
		retryThread.start();
	}

	// Pulls down a fresh list of collections from the media library
	// Returns a boolean of whether the fetch was successful
	public boolean refreshList() {
		try {
			this.collections = api.fetchCollections("/v2/collections");
			this.collections = Arrays.stream(this.collections).filter(collection -> collection.isPlayable).toArray(MediaCollection[]::new);
			System.err.println("DEBUG: New collection list fetched from media api");
			return true;
		} catch (Exception e) {
			System.err.println("ERROR: Can't fetch collections.");
			e.printStackTrace(System.err);
			return false;
		}
	}

	public MediaCollection[] getAllCollections(String currentSlug) {
		for (MediaCollection collection: this.collections) {
			collection.isCurrent = (collection.slug.equals(currentSlug));
		}
		return this.collections;
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