import java.io.* ;
import java.net.* ;
import java.util.* ; 
class NullTrack extends Track{
	public NullTrack() {
		super(null);
	}
	public NullTrack(String url) {
		this();
	}
	public NullTrack(String url, Map<String, String> metadata) {
		this();
	}
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (this.getClass() == other.getClass()) return true;
		return false;
	}
	@Override
	public int hashCode() {
		return 0;
	}
}
