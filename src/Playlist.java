import java.util.*;
import java.lang.reflect.Constructor;

class Playlist {
	private LinkedList<Track> tracks = new LinkedList<Track>();
	private Fetcher fetcher;
	private Thread currentFetcherThread;
	private Loganne loganne;

	// Dropping below this number of tracks triggers a topup
	static final int TOPUP_LIMIT = 10;

	public Playlist(Fetcher fetcher, Loganne loganne) {
		this.fetcher = fetcher;
		fetcher.setPlaylist(this);
		this.loganne = loganne;
		topupTracks();
	}

	public void next() {
		if (tracks.size() > 0) tracks.removeFirst();
		topupTracks();
	}

	public void queue(Track[] tracks) {
		this.tracks.addAll(Arrays.asList(tracks));
	}

	private void topupTracks() {

		// Don't do anything if there's already enough tracks
		if (tracks.size() >= TOPUP_LIMIT) return;

		// Don't do anything if there's already a fetcher thread running
		if (currentFetcherThread != null && currentFetcherThread.isAlive()) return;
		if (loganne != null) loganne.post("fetchTracks", "Fetching more tracks to add to the current playlist");

		currentFetcherThread = new Thread(fetcher);

		currentFetcherThread.start();
	}
}