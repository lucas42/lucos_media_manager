public class CollectionFetcher extends Fetcher {
	private String slug;

	// Constructor
	public CollectionFetcher(String slug) {
		this.slug = slug;
	}
	
	// Implement the run() method of the Runnable interface.
	@Override
	public void run() {
		System.err.println("DEBUG: Fetching random tracks to add to playlist");
		try {
			fetchList();
		} catch (Exception e) {
			System.err.println("ERROR: Can't fetch new tracks.");
			e.printStackTrace(System.err);
		}
	}
	private void fetchList() throws Exception{
		MediaApi api = new MediaApi();
		MediaApiResult result = api.fetchTracks("/v2/collections/" + slug + "/random");
		playlist.queue(result.tracks);
		System.err.println("DEBUG: New tracks added to playlist from " + slug);
	}

	public String getSlug() {
		return slug;
	}

}