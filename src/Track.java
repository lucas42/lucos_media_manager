import java.io.* ;
import java.net.* ;
import java.util.* ; 
import java.math.BigInteger;
class Track {
	protected String url;
	protected Map<String, String> metadata;
	private float currentTime = 0;
	private BigInteger timeSet = null; // The time currentTime was updated (in millisecs since unix epoch)
	public Track(String url) {
		this(url, new HashMap<String, String>());
	}
	public Track(String url, Map<String, String> metadata) {
		this.url = url;
		if (metadata.get("img") == null) {
			if (metadata.get("mbid_artist") != null) metadata.put("img", "https://staticmedia.l42.eu/artists/"+metadata.get("mbid_artist")+".jpg");
			else metadata.put("img", Manager.getSetting("default_img"));
		}
		if (metadata.get("thumb") == null) {
			if (metadata.get("mbid_artist") != null) metadata.put("thumb", "https://staticmedia.l42.eu/artists/"+metadata.get("mbid_artist")+".jpg");
			else metadata.put("thumb", Manager.getSetting("default_thumb"));
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
			if (metadata.get("track_id") != null) metadata.put("editurl", "null?id="+metadata.get("track_id"));
		}
		this.metadata = metadata;
	}
	@Override
	public int hashCode() {
		if (url == null) return 0;
		return url.hashCode();
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
	public String getExtUrl() {
		return metadata.get("exturl");
	}
	public String getEditUrl() {
		return metadata.get("editurl");
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
}
