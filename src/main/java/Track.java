import java.io.*;
import java.net.*;
import java.util.*;
import java.math.BigInteger;
import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;

class Track {
	private transient MediaApi mediaApi;
	private String url;
	private Map<String, String> metadata;
	private float currentTime = 0;
	@SuppressWarnings("unused")
	private BigInteger timeSet = null; // The time currentTime was updated (in millisecs since unix epoch)
	private String uuid;

	public Track(MediaApi mediaApi, String url) {
		this(mediaApi, url, new HashMap<String, String>());
	}

	public Track(MediaApi mediaApi, String url, Map<String, String> metadata) {
		this.mediaApi = mediaApi;
		this.uuid = UUID.randomUUID().toString();
		this.update(url, metadata);
	}

	public void update(String url, Map<String, String> metadata) {
		this.url = url;
		if (metadata.get("img") == null) {
			if (metadata.get("mbid_artist") != null)
				metadata.put("img", "https://staticmedia.l42.eu/artists/" + metadata.get("mbid_artist") + ".jpg");
			else
				metadata.put("img", "https://staticmedia.l42.eu/music-pic.png");
		}
		if (metadata.get("thumb") == null) {
			if (metadata.get("mbid_artist") != null)
				metadata.put("thumb", "https://staticmedia.l42.eu/artists/" + metadata.get("mbid_artist") + ".jpg");
			else
				metadata.put("thumb", "https://staticmedia.l42.eu/music-pic.png");
		}
		if (metadata.get("title") == null) {
			if (url == null)
				metadata.put("title", "No Track");
			else {
				String[] dirs = url.split("/");
				String file = dirs[dirs.length - 1];
				int jj = file.lastIndexOf('.');
				if (jj > -1)
					file = file.substring(0, jj);
				metadata.put("title", file);
			}
		}
		if (metadata.get("exturl") == null) {
			if (metadata.get("pid") != null)
				metadata.put("exturl", "https://www.bbc.co.uk/programmes/" + metadata.get("pid"));
			else if (metadata.get("mbid_artist") != null)
				metadata.put("exturl", "https://musicbrainz.org/artist/" + metadata.get("mbid_artist"));
		}
		if (metadata.get("editurl") == null) {
			if (metadata.get("trackid") != null)
				metadata.put("editurl", "https://media-metadata.l42.eu/tracks/" + metadata.get("trackid"));
		}
		if (metadata.get("added") != null) {
			String added = metadata.get("added");

			// Really hacky way to support dates with no timezone attached (assumes all
			// times are UTC and expressed as 'Z', rather than +0000)
			if (!added.endsWith("Z"))
				added += "Z";
			try {
				Instant addedTime = Instant.parse(added);
				Instant fortnightAgo = Instant.now().minus(Duration.ofDays(14));
				if (addedTime.isAfter(fortnightAgo)) {
					metadata.put("new", "true");
				} else {
					metadata.remove("new");
				}
			} catch (DateTimeParseException e) {
				metadata.remove("new");
			}
		}
		this.metadata = metadata;
	}

	public void update(Track updateTrack) {
		update(updateTrack.getUrl(), updateTrack.getMetadata());
	}

	@Override
	public int hashCode() {
		if (url == null)
			return 0;
		return url.hashCode() + metadata.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (this.getClass() != other.getClass())
			return false;
		Track otherTrack = (Track) other;
		if (url == null)
			return (otherTrack.getUrl() == null);
		return (url.equals(otherTrack.getUrl()));
	}

	public String getUrl() {
		return url;
	}

	public String getMetadata(String key) {
		return metadata.get(key);
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setTime(float newTime, BigInteger newTimeSet) {
		if (newTime > currentTime) {
			currentTime = newTime;
			timeSet = newTimeSet;
		}
	}

	public void timeSetNow() {
		Date date = new Date();
		timeSet = new BigInteger(Long.toString(date.getTime()));
	}

	public float getCurrentTime() {
		return currentTime;
	}

	public void refreshMetadata() throws MalformedURLException, IOException {
		Track latestTrack = this.mediaApi
				.fetchTrack("/v3/tracks?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8));
		this.update(latestTrack.getUrl(), latestTrack.getMetadata());
	}

	/**
	 * Records the current timestamp as a tag on this track in the media metadata API.
	 * Errors are logged but do not propagate — tag recording is best-effort and must not
	 * interfere with the playlist operation that triggered it.
	 *
	 * @param tagName  The tag predicate name (e.g. "lastSuccessfulPlay", "lastSkip", "lastError")
	 */
	void recordTag(String tagName) {
		recordTagWithValue(tagName, Instant.now().toString());
	}

	/**
	 * Records an arbitrary string value as a tag on this track in the media metadata API.
	 * Errors are logged but do not propagate — tag recording is best-effort and must not
	 * interfere with the playlist operation that triggered it.
	 *
	 * @param tagName  The tag predicate name (e.g. "lastErrorMessage")
	 * @param value    The string value to store
	 */
	void recordTagWithValue(String tagName, String value) {
		String trackid = metadata.get("trackid");
		if (trackid == null || mediaApi == null) {
			return;
		}
		String path = "/v3/tracks/" + trackid;
		JsonObject tagValue = new JsonObject();
		tagValue.addProperty("name", value);
		JsonArray values = new JsonArray();
		values.add(tagValue);
		JsonObject tags = new JsonObject();
		tags.add(tagName, values);
		JsonObject body = new JsonObject();
		body.add("tags", tags);
		try {
			mediaApi.patch(path, body.toString());
		} catch (Exception e) {
			System.out.println("WARNING: Failed to record " + tagName + " tag for track " + trackid + ": " + e.getMessage());
		}
	}

	/**
	 * Records two tag values as a single PATCH request, avoiding duplicate Loganne events.
	 * Use this when two tags should be treated atomically by the metadata API (e.g. lastError
	 * paired with lastErrorMessage). Errors are logged but do not propagate.
	 *
	 * @param tagName1  The first tag predicate name (e.g. "lastError")
	 * @param value1    The first value to store
	 * @param tagName2  The second tag predicate name (e.g. "lastErrorMessage")
	 * @param value2    The second value to store
	 */
	void recordTwoTagsWithValues(String tagName1, String value1, String tagName2, String value2) {
		String trackid = metadata.get("trackid");
		if (trackid == null || mediaApi == null) {
			return;
		}
		String path = "/v3/tracks/" + trackid;
		JsonObject tags = new JsonObject();

		JsonObject tagValue1 = new JsonObject();
		tagValue1.addProperty("name", value1);
		JsonArray values1 = new JsonArray();
		values1.add(tagValue1);
		tags.add(tagName1, values1);

		JsonObject tagValue2 = new JsonObject();
		tagValue2.addProperty("name", value2);
		JsonArray values2 = new JsonArray();
		values2.add(tagValue2);
		tags.add(tagName2, values2);

		JsonObject body = new JsonObject();
		body.add("tags", tags);
		try {
			mediaApi.patch(path, body.toString());
		} catch (Exception e) {
			System.out.println("WARNING: Failed to record " + tagName1 + "/" + tagName2 + " tags for track " + trackid + ": " + e.getMessage());
		}
	}

	public String getUuid() {
		return uuid;
	}
}
