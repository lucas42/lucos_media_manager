import java.io.* ;
import java.net.* ;
import java.util.* ; 
import java.math.BigInteger;
import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.nio.charset.StandardCharsets;

class Track {
	private MediaApi mediaApi;
	private String url;
	private Map<String, String> metadata;
	private float currentTime = 0;
	private BigInteger timeSet = null; // The time currentTime was updated (in millisecs since unix epoch)
	public Track(MediaApi mediaApi, String url) {
		this(mediaApi, url, new HashMap<String, String>());
	}
	public Track(MediaApi mediaApi, String url, Map<String, String> metadata) {
		this.mediaApi = mediaApi;
		this.update(url, metadata);
	}
	public void update(String url, Map<String, String> metadata) {
		this.url = url;
		if (metadata.get("img") == null) {
			if (metadata.get("mbid_artist") != null) metadata.put("img", "https://staticmedia.l42.eu/artists/"+metadata.get("mbid_artist")+".jpg");
			else metadata.put("img", "https://staticmedia.l42.eu/music-pic.png");
		}
		if (metadata.get("thumb") == null) {
			if (metadata.get("mbid_artist") != null) metadata.put("thumb", "https://staticmedia.l42.eu/artists/"+metadata.get("mbid_artist")+".jpg");
			else metadata.put("thumb", "https://staticmedia.l42.eu/music-pic.png");
		}
		if (metadata.get("title") == null) {
			if (url == null) metadata.put("title", "No Track");
			else {
				String[] dirs = url.split("/");
				String file = dirs[dirs.length-1];
				int jj = file.lastIndexOf('.');
				if ( jj > -1) file = file.substring(0, jj);
				metadata.put("title", file);
			}
		}
		if (metadata.get("exturl") == null) {
			if (metadata.get("pid") != null) metadata.put("exturl", "https://www.bbc.co.uk/programmes/"+metadata.get("pid"));
			else if(metadata.get("mbid_artist") != null) metadata.put("exturl", "https://musicbrainz.org/artist/"+metadata.get("mbid_artist"));
		}
		if (metadata.get("editurl") == null) {
			if (metadata.get("trackid") != null) metadata.put("editurl", "https://media-metadata.l42.eu/tracks/"+metadata.get("trackid"));
		}
		if (metadata.get("added") != null) {
			String added = metadata.get("added");

			// Really hacky way to support dates with no timezone attached (assumes all times are UTC and expressed as 'Z', rather than +0000)
			if (!added.endsWith("Z")) added += "Z";
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
		if (url == null) return 0;
		return url.hashCode() + metadata.hashCode();
	}
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (this.getClass() != other.getClass()) return false;
		Track otherTrack = (Track)other;
		if (url == null) return (otherTrack.getUrl() == null);
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
		Date date= new Date();
		timeSet = new BigInteger(Long.toString(date.getTime()));
	}
	public float getCurrentTime() {
		return currentTime;
	}
	public void refreshMetadata() throws MalformedURLException, IOException {
		Track latestTrack = this.mediaApi.fetchTrack("/v2/tracks?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8));
		this.update(latestTrack.getUrl(), latestTrack.getMetadata());
	}
}
