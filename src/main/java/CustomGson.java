import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;

class CustomGson {

	/**
	 * Returns a Gson object with a custom deserializer for handling tracks from the media api
	 **/
	public static Gson get(Status status, MediaApi mediaApi) {
		GsonBuilder gsonBuilder = new GsonBuilder();

		JsonDeserializer<Track> trackDeserializer =
			new JsonDeserializer<Track>() {
				@Override
				public Track deserialize(JsonElement json, Type typeOfSrc, JsonDeserializationContext context) {

					JsonObject obj = json.getAsJsonObject();
					String url = obj.get("url").getAsString();

					Map<String, String> metadata = new HashMap<>();
					JsonElement tagsElement = obj.get("tags");
					if (tagsElement != null && tagsElement.isJsonObject()) {
						for (Map.Entry<String, JsonElement> entry : tagsElement.getAsJsonObject().entrySet()) {
							JsonElement value = entry.getValue();
							if (value.isJsonArray()) {
								// V3 structured tags: each predicate maps to an array of {"name": ..., "uri": ...}
								JsonArray arr = value.getAsJsonArray();
								if (arr.size() > 0 && arr.get(0).isJsonObject()) {
									JsonObject first = arr.get(0).getAsJsonObject();
									if (first.has("name")) {
										metadata.put(entry.getKey(), first.get("name").getAsString());
									}
								}
							} else if (value.isJsonPrimitive()) {
								// V2 flat tags: each predicate maps directly to a string value
								metadata.put(entry.getKey(), value.getAsString());
							}
						}
					}

					// V3 uses "id", V2 uses "trackid"
					if (obj.has("id")) {
						metadata.put("trackid", obj.get("id").getAsString());
					} else if (obj.has("trackid")) {
						metadata.put("trackid", obj.get("trackid").getAsString());
					}

					/** The following tag is only for debugging purposes **/
					if (obj.has("weighting")) {
						metadata.put("_track_weighting", obj.get("weighting").getAsString());
					}
					return new Track(mediaApi, url, metadata);
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
	public static Gson get(Status status) {
		return get(status, status.getMediaApi());
	}
	public static Gson get(MediaApi mediaApi) {
		return get(null, mediaApi);
	}
}

class LoganneTrackEvent {
	Track track;
	String type;
}
