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
class ControllerV2Test {

	void compareRequestResponse(Status status, String requestInput, String responseSnippet) {
		try {
			Socket socket = mock(Socket.class);
			InetAddress mockedAddress = mock(InetAddress.class);
			when(mockedAddress.getHostName()).thenReturn("test.host");
			when(socket.getInetAddress()).thenReturn(mockedAddress);
			HttpRequest request = new HttpRequest(socket);
			Controller controller = new ControllerV2(status, request);
			InputStream input = new StringBufferInputStream(requestInput);
			when(socket.getInputStream()).thenReturn(input);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			when(socket.getOutputStream()).thenReturn(output);
			request.readFromSocket();
			controller.processRequest();
			assertTrue(output.toString().contains(responseSnippet), "response snippet ("+responseSnippet+") not found in response:\n"+output.toString());
		} catch (Exception e) {
			fail("Unexpected HTTP Error", e);
		}
	}
	void compareRequestResponse(String request, String responseSnippet) {
		compareRequestResponse(new Status(mock(Playlist.class), new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class)), request, responseSnippet);
	}

	@Test
	void notFound() {
		compareRequestResponse("GET /unknown HTTP/1.1\n", "404 Not Found");
	}
	@Test
	void robots() {
		compareRequestResponse("Get /robots.txt HTTP/1.1\n", "Disallow: /");
	}
	@Test
	void playPause() {
		Status status = new Status(mock(Playlist.class), new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));
		status.setPlaying(true);
		compareRequestResponse(status, "POST /pause HTTP/1.0\n", "204");
		assertFalse(status.getPlaying());
		compareRequestResponse(status, "POST /pause HTTP/1.0\n", "204");
		assertFalse(status.getPlaying());
		compareRequestResponse(status, "POST /play HTTP/1.0\n", "204");
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "POST /play HTTP/1.0\n", "204");
		assertTrue(status.getPlaying());
	}
	@Test
	void volume() {
		Status status = new Status(mock(Playlist.class), new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));

		// The following values should update the volume
		compareRequestResponse(status, "POST /volume?volume=1.0 HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "volume\":1");
		compareRequestResponse(status, "POST /volume?volume=0 HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "volume\":0");
		compareRequestResponse(status, "POST /volume?volume=0.7 HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "volume\":0.7");

		// The following values should all fail, and not update the volume
		compareRequestResponse(status, "POST /volume?volume=string HTTP/1.1", "400");
		compareRequestResponse(status, "POST /volume?volume=NaN HTTP/1.1", "400");
		compareRequestResponse(status, "POST /volume?volume=-1 HTTP/1.1", "400");
		compareRequestResponse(status, "POST /volume?volume=1.3 HTTP/1.1", "400");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "volume\":0.7");
	}
	@Test
	void devices() {
		Status status = new Status(mock(Playlist.class), new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));
		status.setPlaying(false);
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[]");
		compareRequestResponse(status, "POST /devices?uuid=46eca36b-2e4f-46bd-a756-249c45850cac HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Device 1\",\"isDefaultName\":true,\"isCurrent\":true,\"isConnected\":false}]");
		compareRequestResponse(status, "POST /devices?uuid=46eca36b-2e4f-46bd-a756-249c45850cac&name=Flying%20Fidget HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":false}]");
		compareRequestResponse(status, "POST /devices?uuid=3f03fae2-7a79-4ca1-9593-07c080f8402a&name=Jolly%20Jumper HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":false},{\"uuid\":\"3f03fae2-7a79-4ca1-9593-07c080f8402a\",\"name\":\"Jolly Jumper\",\"isDefaultName\":false,\"isCurrent\":false,\"isConnected\":false}]");
		// Changing the current device without a 'play' param, should:
		// * set that device's isCurrent to be true
		// * set the other devices' isCurrent to be false
		// * Not affect the isPlaying status
		compareRequestResponse(status, "POST /devices/current?uuid=3f03fae2-7a79-4ca1-9593-07c080f8402a HTTP/1.1", "204");
		assertFalse(status.getPlaying());
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":false,\"isConnected\":false},{\"uuid\":\"3f03fae2-7a79-4ca1-9593-07c080f8402a\",\"name\":\"Jolly Jumper\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":false}]");
		compareRequestResponse(status, "GET /poll/summary?device=46eca36b-2e4f-46bd-a756-249c45850cac HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":false,\"isConnected\":true},{\"uuid\":\"3f03fae2-7a79-4ca1-9593-07c080f8402a\",\"name\":\"Jolly Jumper\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":false}]");
		// Changing the current device with 'play' param set to true, should:
		// * set that device's isCurrent to be true
		// * set the other devices' isCurrent to be false
		// * Change the isPlaying status to true
		compareRequestResponse(status, "POST /devices/current?uuid=46eca36b-2e4f-46bd-a756-249c45850cac&play=true HTTP/1.1", "204");
		assertTrue(status.getPlaying());
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":true},{\"uuid\":\"3f03fae2-7a79-4ca1-9593-07c080f8402a\",\"name\":\"Jolly Jumper\",\"isDefaultName\":false,\"isCurrent\":false,\"isConnected\":false}]");
		// Setting current device to the one which is already current with 'play' param set to true, should:
		// * have no affect on an devices' isCurrent status
		// * Change the isPlaying status to true
		status.setPlaying(false);
		compareRequestResponse(status, "POST /devices/current?uuid=46eca36b-2e4f-46bd-a756-249c45850cac&play=true HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":true},{\"uuid\":\"3f03fae2-7a79-4ca1-9593-07c080f8402a\",\"name\":\"Jolly Jumper\",\"isDefaultName\":false,\"isCurrent\":false,\"isConnected\":false}]");
		assertTrue(status.getPlaying());
		// Setting current device to the one which is already current with 'play' param set to true when aleady playing, should:
		// * Make no change to status.
		status.setPlaying(false);
		compareRequestResponse(status, "POST /devices/current?uuid=46eca36b-2e4f-46bd-a756-249c45850cac&play=true HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":true},{\"uuid\":\"3f03fae2-7a79-4ca1-9593-07c080f8402a\",\"name\":\"Jolly Jumper\",\"isDefaultName\":false,\"isCurrent\":false,\"isConnected\":false}]");
		assertTrue(status.getPlaying());
	}
	@Test
	void trackUpdated() {
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
		int startingHashCode = status.hashCode();

		compareRequestResponse(status, "POST /trackUpdated HTTP/1.1\nContent-Length: 325\n\n{\n	humanReadable: \"Track #1347 updated\",\n	source: \"test_updater\",\n	track: {\n		fingerprint: \"abcfxx\",\n		duration: 150,\n		url: \"http://example.com/track/1347\",\n		trackid: 1347,\n		tags: {\n			artist: \"Dolly Parton\",\n			title: \"Stairway To Heaven\"\n		},\n		weighting: 7\n	},\n	type: \"trackUpdated\",\n	date: \"2021-03-27T22:28:45.716Z\"\n}", "204 No Content");

		assertEquals(trackNoChange.getUrl(), "http://example.com/track/8532");
		assertEquals(trackNoChange.getMetadata("artist"), "Beautiful South");
		assertEquals(trackNoChange.getMetadata("trackid"), "8532");

		assertEquals(trackToChange.getUrl(), "http://example.com/track/1347");
		assertEquals(trackToChange.getMetadata("artist"), "Dolly Parton");
		assertEquals(trackToChange.getMetadata("trackid"), "1347");

		assertNotEquals(status.hashCode(), startingHashCode);

		compareRequestResponse(status, "POST /trackUpdated HTTP/1.1\nContent-Length: 3\n\n{\"}","400 Bad Request");

		assertEquals(trackNoChange.getUrl(), "http://example.com/track/8532");
		assertEquals(trackNoChange.getMetadata("artist"), "Beautiful South");
		assertEquals(trackNoChange.getMetadata("trackid"), "8532");
	}
	@Test
	void trackDeleted() {
		Fetcher fetcher = mock(RandomFetcher.class);
		Loganne loganne = mock(Loganne.class);

		// Create playlist with 4 tracks, 2 of which are the same track
		Playlist playlist = new Playlist(fetcher, loganne);
		Status status = new Status(playlist, new DeviceList(null), mock(CollectionList.class), mock(MediaApi.class));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347"))));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/8532", new HashMap<String, String>(Map.of("title", "Good as Gold", "artist", "Beautiful South", "trackid", "8532"))));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/1347", new HashMap<String, String>(Map.of("title", "Stairway To Heaven", "artist", "Led Zeplin", "trackid", "1347"))));
		playlist.queueNext(new Track(status.getMediaApi(), "http://example.com/track/8533", new HashMap<String, String>(Map.of("title", "Old Red Eyes Is Back", "artist", "Beautiful South", "trackid", "8533"))));

		int startingHashCode = status.hashCode();

		compareRequestResponse(status, "POST /trackDeleted HTTP/1.1\nContent-Length: 289\n\n{\"humanReadable\":\"Track #1347 deleted\",\"source\":\"test_updater\",\"track\":{\"fingerprint\":\"abcfxx\",\"duration\":73,\"url\":\"http://example.com/track/1347\",\"trackid\":1347,\"tags\":{\"title\":\"Stairway to Heaven\"},\"weighting\":0,\"collections\":[]},\"type\":\"trackDeleted\",\"date\":\"2024-01-27T16:18:47.676Z\"}", "204 No Content");

		assertEquals(2, playlist.getLength());
		assertNotEquals("Stairway To Heaven", playlist.getCurrentTrack().getMetadata("title"));
		assertNotEquals("Stairway To Heaven", playlist.getNextTrack().getMetadata("title"));

		assertNotEquals(startingHashCode, status.hashCode());

		compareRequestResponse(status, "POST /trackDeleted HTTP/1.1\nContent-Length: 3\n\n{\"}","400 Bad Request");

		assertEquals(2, playlist.getLength());
	}

}