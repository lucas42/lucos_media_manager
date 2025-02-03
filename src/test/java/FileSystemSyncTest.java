import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.net.*;
import java.io.*;
import java.util.*;
class FileSystemSyncTest {

	@AfterEach
	void removeStateFile() {
		File stateFile = new File("manager-status.json");
		stateFile.delete();
	}

	@Test
	void statusWrittenAndRead() throws Exception {
		FileSystemSync fsSync = new FileSystemSync(".");
		DeviceList inputDeviceList = new DeviceList(mock(Loganne.class));

		// Connected; current device
		HttpRequest requestFromCurrentDevice = mock(HttpRequest.class);
		when(requestFromCurrentDevice.removeParam("device")).thenReturn("d4af03fa-8e86-4dc3-8df9-08c2879c32bf");
		inputDeviceList.updateDevice("d4af03fa-8e86-4dc3-8df9-08c2879c32bf", "Main Device");
		inputDeviceList.setCurrent("d4af03fa-8e86-4dc3-8df9-08c2879c32bf");
		inputDeviceList.openConnection(requestFromCurrentDevice);

		// Unconnected, nameless, not current device
		inputDeviceList.getDevice("6881a1af-f7d2-4128-af7a-81d51c3e48d3");

		// Connected; not current device
		HttpRequest requestFromNotCurrentDevice = mock(HttpRequest.class);
		when(requestFromNotCurrentDevice.removeParam("device")).thenReturn("2a9a10f1-f26f-4541-93f4-69ee8e28ba89");
		inputDeviceList.updateDevice("2a9a10f1-f26f-4541-93f4-69ee8e28ba89", "Other Device");
		inputDeviceList.openConnection(requestFromNotCurrentDevice);

		Playlist playlist = mock(Playlist.class);
		when(playlist.getCurrentFetcherSlug()).thenReturn("pears");
		List<Track> inputTracks = new ArrayList<Track>();
		inputTracks.add(new Track(mock(MediaApi.class), "https://example.com/track-1.mp3"));
		inputTracks.add(new Track(mock(MediaApi.class), "https://example.com/track-2.mp3", new HashMap<>(Map.of("title", "Old Name"))));
		when(playlist.getTracks()).thenReturn(inputTracks);

		Status inputStatus = new Status(playlist, inputDeviceList, mock(CollectionList.class), mock(MediaApi.class), fsSync);
		inputStatus.setPlaying(false);
		inputStatus.setVolume(0.7f);

		inputStatus.syncToFileSystem();

		MediaApi api = mock(MediaApi.class);
		when(api.fetchCollections("/v2/collections")).thenReturn(new MediaCollection[0]);
		MediaApiResult topupResult = new MediaApiResult();
		Track[] topupTracks = {new Track(api, "https://example.com/track-3.mp3", new HashMap<>(Map.of("title", "Top-up Track")))};
		topupResult.tracks = topupTracks;
		when(api.fetchTracks("/v2/collections/pears/random")).thenReturn(topupResult);
		when(api.fetchTrack("/v2/tracks?url=https%3A%2F%2Fexample.com%2Ftrack-1.mp3")).thenReturn(new Track(api, "https://example.com/track-1.mp3", new HashMap<>()));
		when(api.fetchTrack("/v2/tracks?url=https%3A%2F%2Fexample.com%2Ftrack-2.mp3")).thenReturn(new Track(api, "https://example.com/track-2.mp3", new HashMap<>(Map.of("title", "New Name"))));

		Status outputStatus = fsSync.readStatus(mock(Loganne.class), api);
		Thread.sleep(100); // HACK: Some playlist fetching happens asynchronously, so wait for that

		assertEquals(0.7, outputStatus.getVolume(), 0.02);
		assertEquals(false, outputStatus.getPlaying());

		Device[] outputDevices = outputStatus.getDeviceList().getAllDevices();
		assertEquals(3, outputDevices.length);
		assertEquals("Main Device", outputDevices[0].getName());
		assertEquals("Device 2", outputDevices[1].getName());
		assertEquals("Other Device", outputDevices[2].getName());
		assertTrue(outputDevices[0].isCurrent());
		assertFalse(outputDevices[1].isCurrent());
		assertFalse(outputDevices[2].isCurrent());

		// No devices should be marked as connected on app startup
		assertFalse(outputStatus.getDeviceList().isConnected(outputDevices[0]));
		assertFalse(outputStatus.getDeviceList().isConnected(outputDevices[1]));
		assertFalse(outputStatus.getDeviceList().isConnected(outputDevices[2]));

		assertEquals("pears", outputStatus.getPlaylist().getCurrentFetcherSlug());

		List<Track> outputTracks = outputStatus.getPlaylist().getTracks();
		assertEquals(3, outputTracks.size());
		assertEquals("https://example.com/track-1.mp3", outputTracks.get(0).getUrl());
		assertEquals("https://example.com/track-2.mp3", outputTracks.get(1).getUrl());
		assertEquals("https://example.com/track-3.mp3", outputTracks.get(2).getUrl());
		assertEquals("New Name", outputTracks.get(1).getMetadata("title"));
		assertEquals("Top-up Track", outputTracks.get(2).getMetadata("title"));

		verify(api, never()).fetchTracks("/v2/tracks/random");

	}
}