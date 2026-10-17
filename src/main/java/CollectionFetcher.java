public class CollectionFetcher extends Fetcher {
	private String slug;
	private CollectionList collectionList;
	private MediaApi api;

	public CollectionFetcher(MediaApi api, String slug, CollectionList collectionList) {
		this.slug = slug;
		this.collectionList = collectionList;
		this.api = api;
	}
	
	@Override
	Track[] fetchBatch() throws Exception {
		System.err.println("DEBUG: Fetching random tracks from " + slug + " to add to playlist");
		return api.fetchTracks("/v3/collections/" + slug + "/random").tracks;
	}

	public String getSlug() {
		return slug;
	}

	public String getName() {
		return collectionList.getNameForSlug(slug);
	}

}