public class RandomFetcher extends Fetcher {
	private MediaApi api;

	// Constructor
	public RandomFetcher(MediaApi api) {
		this.api = api;
	}

	// Implement the run() method of the Runnable interface.
	@Override
	public void run() {
		System.err.println("DEBUG: Fetching random tracks to add to playlist");
		try {
			fetchList();
			this.fetchComplete();
		} catch (Exception e) {
			System.err.println("ERROR: Can't fetch new tracks.");
			e.printStackTrace(System.err);
		}
	}

	private void fetchList() throws Exception {
		System.out.println("CALLING /v2/tracks/random");
		MediaApiResult result = api.fetchTracks("/v2/tracks/random");
		playlist.queue(result.tracks);
		System.err.println("DEBUG: New tracks added to playlist");
	}

	public String getSlug() {
		return "all"; // The metadata API treats this is a reserved slug, so it should never clash
						// with a collection
	}
}