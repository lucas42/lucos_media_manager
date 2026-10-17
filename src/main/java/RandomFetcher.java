public class RandomFetcher extends Fetcher {
	private MediaApi api;

	// Constructor
	public RandomFetcher(MediaApi api) {
		this.api = api;
	}

	@Override
	Track[] fetchList() throws Exception {
		System.err.println("DEBUG: Fetching random tracks to add to playlist");
		return api.fetchTracks("/v3/tracks/random").tracks;
	}

	public String getSlug() {
		return "all"; // The metadata API treats this is a reserved slug, so it should never clash
						// with a collection
	}

	public String getName() {
		return "All Music";
	}
}