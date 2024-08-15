import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

import java.net.*;
import java.io.*;
import java.util.*;
class WebhookControllerTest {
	void compareRequestResponse(Status status, String path, Method method, String requestBody, int responseStatus, String responseString, String contentType, String responseBody) throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn(path);
		when(request.getMethod()).thenReturn(method);
		if (requestBody == null) requestBody = "";
		when(request.getData()).thenReturn(requestBody);
		Controller controller = new FrontController(status, request);
		controller.run();
		if (contentType != null) verify(request).sendHeaders(responseStatus, responseString, contentType);
		else verify(request).sendHeaders(responseStatus, responseString);
		if (responseBody != null) verify(request).writeBody(responseBody);
		verify(request).close();
	}
	void checkNotAllowed(Status status, String path, Method method, Map<String, String> getParameters, Collection<Method> allowedMethods) throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getPath()).thenReturn(path);
		when(request.getMethod()).thenReturn(method);
		Controller controller = new FrontController(status, request);
		controller.run();

		verify(request).notAllowed(allowedMethods);
	}

	@Test
	void unknownPathReturns404() throws Exception {
		Status status = new Status(null, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));
		compareRequestResponse(status, "/webhooks/unknown", Method.POST, null, 404, "Not Found", "text/plain", "Can't find webhook \"unknown\"");
	}

	@Test
	void trackUpdated() throws Exception {
		Fetcher fetcher = mock(RandomFetcher.class);
		Loganne loganne = mock(Loganne.class);
		Playlist playlist = new Playlist(fetcher, loganne);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));
		Map<String, String> initialMetadata = new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347"));
		Map<String, String> noChangeMetadata = new HashMap<String, String>(Map.of("title", "Good as Gold", "artist", "Beautiful South", "trackid", "8532"));
		Track trackToChange = new Track(status.getMediaApi(), "http://example.com/track/1337",initialMetadata);
		Track trackNoChange = new Track(status.getMediaApi(), "http://example.com/track/8532", noChangeMetadata);
		playlist.queueNext(trackToChange);
		playlist.queueNext(trackNoChange);
		int hashCode = status.hashCode();

		compareRequestResponse(status, "/webhooks/trackUpdated", Method.POST, "{\n	humanReadable: \"Track #1347 updated\",\n	source: \"test_updater\",\n	track: {\n		fingerprint: \"abcfxx\",\n		duration: 150,\n		url: \"http://example.com/track/1347\",\n		trackid: 1347,\n		tags: {\n			artist: \"Dolly Parton\",\n			title: \"Stairway To Heaven\"\n		},\n		weighting: 7\n	},\n	type: \"trackUpdated\",\n	date: \"2021-03-27T22:28:45.716Z\"\n}", 204, "No Content", null, null);

		assertEquals(trackNoChange.getUrl(), "http://example.com/track/8532");
		assertEquals(trackNoChange.getMetadata("artist"), "Beautiful South");
		assertEquals(trackNoChange.getMetadata("trackid"), "8532");

		assertEquals(trackToChange.getUrl(), "http://example.com/track/1347");
		assertEquals(trackToChange.getMetadata("artist"), "Dolly Parton");
		assertEquals(trackToChange.getMetadata("trackid"), "1347");

		assertNotEquals(status.hashCode(), hashCode);
		hashCode = status.hashCode();

		compareRequestResponse(status, "/webhooks/trackUpdated", Method.POST, "{\"}", 400, "Bad Request", null, null);


		assertEquals(trackNoChange.getUrl(), "http://example.com/track/8532");
		assertEquals(trackNoChange.getMetadata("artist"), "Beautiful South");
		assertEquals(trackNoChange.getMetadata("trackid"), "8532");
		assertEquals(status.hashCode(), hashCode);

		checkNotAllowed(status, "/webhooks/trackUpdated", Method.PUT, null, Arrays.asList(Method.POST));
	}
	@Test
	void trackDeleted() throws Exception {
		Fetcher fetcher = mock(RandomFetcher.class);
		Loganne loganne = mock(Loganne.class);

		// Create playlist with 4 tracks, 2 of which are the same track
		Playlist playlist = new Playlist(fetcher, loganne);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347"))));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/8532", new HashMap<String, String>(Map.of("title", "Good as Gold", "artist", "Beautiful South", "trackid", "8532"))));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347"))));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/8533", new HashMap<String, String>(Map.of("title", "Old Red Eyes Is Back", "artist", "Beautiful South", "trackid", "8533"))));

		int hashCode = status.hashCode();

		compareRequestResponse(status, "/webhooks/trackDeleted", Method.POST, "{\"humanReadable\":\"Track #1347 deleted\",\"source\":\"test_updater\",\"track\":{\"fingerprint\":\"abcfxx\",\"duration\":73,\"url\":\"http://example.com/track/1347\",\"trackid\":1347,\"tags\":{\"title\":\"Stairway to Heaven\"},\"weighting\":0,\"collections\":[]},\"type\":\"trackDeleted\",\"date\":\"2024-01-27T16:18:47.676Z\"}", 204, "No Content", null, null);

		assertEquals(2, playlist.getLength());
		assertNotEquals("Stairway To Heaven", playlist.getCurrentTrack().getMetadata("title"));
		assertNotEquals("Stairway To Heaven", playlist.getNextTrack().getMetadata("title"));

		assertNotEquals(hashCode, status.hashCode());
		hashCode = status.hashCode();

		compareRequestResponse(status, "/webhooks/trackDeleted", Method.POST, "{\"}", 400, "Bad Request", null, null);

		assertEquals(2, playlist.getLength());
		assertEquals(status.hashCode(), hashCode);

		checkNotAllowed(status, "/webhooks/trackDeleted", Method.DELETE, null, Arrays.asList(Method.POST));
	}
	@Test
	void collectionsChanged() throws Exception {
		CollectionList collectionList = mock(CollectionList.class);
		when(collectionList.refreshList()).thenReturn(true);

		Status status = new Status(new Playlist(mock(RandomFetcher.class), mock(Loganne.class)), new DeviceList(null), collectionList, mock(MediaApi.class));

		compareRequestResponse(status, "/webhooks/collectionCreated", Method.POST, "{\"humanReadable\":\"New Collection Created\",\"source\":\"test_updater\",\"date\":\"2024-07-27T16:18:47.676Z\"}", 204, "No Content", null, null);
		verify(collectionList, times(1)).refreshList();
		compareRequestResponse(status, "/webhooks/collectionUpdated", Method.POST, "{\"humanReadable\":\"Collection Updated\",\"source\":\"test_updater\",\"date\":\"2024-07-27T16:18:48.676Z\"}", 204, "No Content", null, null);
		verify(collectionList, times(2)).refreshList();
		compareRequestResponse(status, "/webhooks/collectionDeleted", Method.POST, "{\"humanReadable\":\"Collection Deleted\",\"source\":\"test_updater\",\"date\":\"2024-07-27T16:18:49.676Z\"}", 204, "No Content", null, null);
		verify(collectionList, times(3)).refreshList();

		checkNotAllowed(status, "/webhooks/collectionUpdated", Method.PATCH, null, Arrays.asList(Method.POST));
		verify(collectionList, times(3)).refreshList();

		when(collectionList.refreshList()).thenReturn(false);
		compareRequestResponse(status, "/webhooks/collectionDeleted", Method.POST, "{\"humanReadable\":\"Weird Collection Deleted\",\"source\":\"test_updater\",\"date\":\"2024-07-27T16:18:49.676Z\"}", 500, "Internal Server Error", "text/plain", "Failed to fetch collections from media API");
		verify(collectionList, times(4)).refreshList();
	}

}