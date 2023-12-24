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
		this.loganne = loganne;
		setFetcher(fetcher);
	}

	/**
	 * Removes the first instance of a given track from the playlist
	 * (Queues additional tracks if necessary)
	 * @returns boolean Whether or not the track was found in the playlist
	 */
	private boolean removeTrack(Track track) {
		boolean trackRemoved = tracks.remove(track);
		topupTracks();
		return trackRemoved;
	}

	// Called when a track gets to the end of its playback
	public boolean completeTrack(Track track) {
		return removeTrack(track);
	}
	// Called when loading or playback of a track encounters an error
	public boolean flagTrackAsError(Track track, String errorMessage) {
		boolean trackRemoved = removeTrack(track);
		if (trackRemoved) {
			System.out.println("NOTICE: Track "+track.getUrl()+" flagged with error: "+errorMessage);
			// TODO: Record the error somewhere more presistent than application log
		}
		return trackRemoved;
	}
	// Called when a track is deliberatly skipped
	public boolean skipTrack(Track track) {
		return removeTrack(track);
	}

	/**
	 * Removes the first track from the playlist
	 * (Queues additional tracks if necessary)
	 */
	public void skipTrack() {
		if (tracks.size() > 0) tracks.removeFirst();
		topupTracks();
	}

	/**
	 * Removes all the tracks from the playlist
	 * Queues additional
	 */
	private void removeAllTracks() {
		tracks = new LinkedList<Track>();
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

	public void updateTracks(String trackid, Track trackUpdate) {
		for (Track track : tracks) {
			if (track.getMetadata("trackid").equals(trackid)) {
				track.update(trackUpdate);
			}
		}
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

	/**
	 * Sets the fetcher for this playlist
	 * Also, clears all the current tracks and queues fresh ones from the new fetcher
	 */
	public void setFetcher(Fetcher fetcher) {
		this.fetcher = fetcher;
		fetcher.setPlaylist(this);
		removeAllTracks();
	}
}