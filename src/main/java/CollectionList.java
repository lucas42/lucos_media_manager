import java.io.* ;
import java.net.* ;
import com.google.gson.*;

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
			Gson gson = CustomGson.get();
			URL queryUrl = new URL("https://media-api.l42.eu/v2/collections");
			InputStreamReader reader = new InputStreamReader(queryUrl.openStream());
			this.collections = gson.fromJson(reader, MediaCollection[].class);
			for (MediaCollection collection: this.collections) {
				collection.editurl = "https://media-metadata.l42.eu/collections/"+collection.slug;
			}
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
	String editurl;
	boolean isCurrent;
}