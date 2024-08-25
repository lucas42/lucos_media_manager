import java.util.Arrays;

/**
 * Represents the list of available collections in the media library
 */
class CollectionList {
	private MediaCollection[] collections;
	public CollectionList() {
		this.refreshList();
	}

	// Pulls down a fresh list of collections from the media library
	// Returns a boolean of whether the fetch was successful
	public boolean refreshList() {
		try {
			MediaApi api = new MediaApi();
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
		return collections.hashCode();
	}
}

class MediaCollection {
	String slug;
	String name;
	int totalTracks;
	int totalPages;
	boolean isPlayable;
	boolean isCurrent;
}