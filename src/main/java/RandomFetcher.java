public class RandomFetcher extends Fetcher {

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
		MediaApiResult result = api.fetchTracks("/v2/tracks/random");
		playlist.queue(result.tracks);
		System.err.println("DEBUG: New tracks added to playlist");
	}
}