import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.LinkedList;

class CustomGson {

	/**
	 * Returns a Gson object with a custom deserializer for handling tracks from the media api
	 **/
	public static Gson get(Status status) {
		GsonBuilder gsonBuilder = new GsonBuilder();

		JsonDeserializer<Track> trackDeserializer =
			new JsonDeserializer<Track>() {
				@Override
				public Track deserialize(JsonElement json, Type typeOfSrc, JsonDeserializationContext context) {

					String url = json.getAsJsonObject().get("url").getAsString();
					Map<String, String> metadata = context.deserialize(json.getAsJsonObject().get("tags"), Map.class);
					metadata.put("trackid", json.getAsJsonObject().get("trackid").getAsString());

					/** The following tags are only for debugging purposes **/
					if (json.getAsJsonObject().has("weighting")) {
						metadata.put("_track_weighting", json.getAsJsonObject().get("weighting").getAsString());
					}
					if (json.getAsJsonObject().has("_random_weighting")) {
						metadata.put("_random_weighting", json.getAsJsonObject().get("_random_weighting").getAsString());
					}
					if (json.getAsJsonObject().has("_cum_weighting")) {
						metadata.put("_cum_weighting", json.getAsJsonObject().get("_cum_weighting").getAsString());
					}
					return new Track(url, metadata);
				}
			};
		gsonBuilder.registerTypeAdapter(Track.class, trackDeserializer);

		JsonSerializer<Playlist> playlistSerializer =
			new JsonSerializer<Playlist>() {
				@Override
				public JsonElement serialize(Playlist src, Type typeOfSrc, JsonSerializationContext context) {
					return context.serialize(src.getTracks(), LinkedList.class);
				}
			};
		gsonBuilder.registerTypeAdapter(Playlist.class, playlistSerializer);

		JsonSerializer<Device> deviceSerializer =
			new JsonSerializer<Device>() {
				@Override
				public JsonElement serialize(Device src, Type typeOfSrc, JsonSerializationContext context) {
					JsonObject tree = (JsonObject)new Gson().toJsonTree(src);
					if (status != null) tree.addProperty("isConnected", status.getDeviceList().isConnected(src));
					return tree;
				}
			};
		gsonBuilder.registerTypeAdapter(Device.class, deviceSerializer);

		return gsonBuilder.create();
	}
	public static Gson get() {
		return get(null);
	}
}

class LoganneTrackEvent {
	Track track;
	String type;
}