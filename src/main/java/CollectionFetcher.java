public class CollectionFetcher extends Fetcher {
	private String slug;
	private String name;
	private MediaApi api;

	// Constructor
	public CollectionFetcher(MediaApi api, String slug) {
		this(api, slug, null);
	}

	public CollectionFetcher(MediaApi api, String slug, String name) {
		this.slug = slug;
		this.name = (name != null) ? name : slug;
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
		return name;
	}

}