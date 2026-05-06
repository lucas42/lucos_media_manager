public class CollectionFetcher extends Fetcher {
	private String slug;
	private CollectionList collectionList;
	private MediaApi api;

	// Constructor (without CollectionList — getName() falls back to slug)
	public CollectionFetcher(MediaApi api, String slug) {
		this(api, slug, null);
	}

	public CollectionFetcher(MediaApi api, String slug, CollectionList collectionList) {
		this.slug = slug;
		this.collectionList = collectionList;
		this.api = api;
	}
	
	// Implement the run() method of the Runnable interface.
	@Override
	public void run() {
		System.err.println("DEBUG: Fetching random tracks from " + slug + " to add to playlist");
		try {
			fetchList();
			this.fetchComplete();
		} catch (Exception e) {
			System.err.println("ERROR: Can't fetch new tracks.");
			e.printStackTrace(System.err);
		}
	}
	private void fetchList() throws Exception{
		MediaApiResult result = api.fetchTracks("/v3/collections/" + slug + "/random");
		playlist.queue(result.tracks);
		System.err.println("DEBUG: New tracks added to playlist from " + slug);
	}

	public String getSlug() {
		return slug;
	}

	public String getName() {
		if (collectionList != null) return collectionList.getNameForSlug(slug);
		return slug;
	}

}