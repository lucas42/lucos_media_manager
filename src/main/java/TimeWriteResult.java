/**
 * Result of a {@link Playlist#setTrackTimeByUuid} call, carrying enough information
 * for the caller to log the write in full without needing a second lookup.
 */
class TimeWriteResult {
	/** Sentinel for when the track uuid was not found in the playlist. */
	static final TimeWriteResult TRACK_NOT_FOUND = new TimeWriteResult(false, 0f, false);

	/** Whether the track was found in the playlist. */
	final boolean trackFound;

	/** The track's current-time value immediately before this write attempt. */
	final float oldTime;

	/**
	 * Whether the write was accepted (true) or clamped by the monotonic guard (false).
	 * Always false when {@code trackFound} is false.
	 */
	final boolean accepted;

	TimeWriteResult(boolean trackFound, float oldTime, boolean accepted) {
		this.trackFound = trackFound;
		this.oldTime = oldTime;
		this.accepted = accepted;
	}
}
