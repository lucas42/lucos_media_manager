import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class CollectionListTest {

	private long originalRetryInterval;

	@BeforeEach
	void saveRetryInterval() {
		originalRetryInterval = CollectionList.RETRY_INTERVAL_MS;
	}

	@AfterEach
	void restoreRetryInterval() {
		CollectionList.RETRY_INTERVAL_MS = originalRetryInterval;
	}

	@Test
	void successfulStartupDoesNotRetry() throws Exception {
		MediaApi api = mock(MediaApi.class);
		when(api.fetchCollections("/v2/collections")).thenReturn(new MediaCollection[]{});

		CollectionList.RETRY_INTERVAL_MS = 50;
		new CollectionList(api);

		// Wait briefly to confirm no retry fires
		Thread.sleep(200);

		verify(api, times(1)).fetchCollections("/v2/collections");
	}

	@Test
	void failedStartupIsRetried() throws Exception {
		MediaApi api = mock(MediaApi.class);
		CountDownLatch retried = new CountDownLatch(1);

		when(api.fetchCollections("/v2/collections"))
			.thenThrow(new RuntimeException("Connection refused"))
			.thenAnswer(invocation -> {
				retried.countDown();
				return new MediaCollection[]{};
			});

		CollectionList.RETRY_INTERVAL_MS = 50;
		new CollectionList(api);

		assertTrue(retried.await(2, TimeUnit.SECONDS), "Retry should have fired within 2 seconds");
		verify(api, times(2)).fetchCollections("/v2/collections");
	}

	@Test
	void retryStopsAfterSuccess() throws Exception {
		MediaApi api = mock(MediaApi.class);
		CountDownLatch succeeded = new CountDownLatch(1);

		when(api.fetchCollections("/v2/collections"))
			.thenThrow(new RuntimeException("Connection refused"))
			.thenAnswer(invocation -> {
				succeeded.countDown();
				return new MediaCollection[]{};
			});

		CollectionList.RETRY_INTERVAL_MS = 50;
		new CollectionList(api);

		assertTrue(succeeded.await(2, TimeUnit.SECONDS), "Retry should have succeeded within 2 seconds");

		// Give the retry thread time to potentially fire again
		Thread.sleep(200);

		// Should have been called exactly twice: once at startup (failed), once in retry (succeeded)
		verify(api, times(2)).fetchCollections("/v2/collections");
	}
}
