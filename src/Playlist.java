import java.util.*;
import java.lang.reflect.Constructor;
import java.math.BigInteger;

class Playlist {
	private LinkedList<Track> tracks = new LinkedList<Track>();
	private transient Fetcher fetcher;
	private transient Thread currentFetcherThread;
	private transient Loganne loganne;

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
	public void finished(Track track, String trackstatus) {
		tracks.remove(track);
		topupTracks();
	}

	public void queue(Track[] tracks) {
		this.tracks.addAll(Arrays.asList(tracks));
	}
	public void queueNow(Track track) {
		this.tracks.addFirst(track);
	}
	public void queueNext(Track track) {
		if (tracks.size() == 0) queueNow(track);
		else this.tracks.add(1, track);
	}
	public void queueEnd(Track track) {
		this.tracks.add(track);
	}

	public int getLength() {
		return tracks.size();
	}

	public Collection<Track> getTracks() {
		return tracks;
	}
	public Track getCurrentTrack() {
		if (tracks.size() == 0) return null;
		return tracks.getFirst();
	}
	public Track getNextTrack() {
		if (tracks.size() <= 1) return null;
		return tracks.get(1);
	}

	public boolean setTrackTime(Track track, float time, BigInteger timeSet) {
		// Searches through the tracks for one which matches (ie has same URL) as one passed in
		int index = tracks.indexOf(track);
		if (index == -1) return false;

		// Update the exact instance of the track in the playlist (rather than just an equivalent one)
		Track playlistTrack = tracks.get(index);
		playlistTrack.setTime(time, timeSet);
		return true;
	}


	@Override
	public int hashCode() {
		return tracks.hashCode();
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