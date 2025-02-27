import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Timeout.ThreadMode.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.Arrays;
import java.util.Map;

class LongPollControllerV3Test {

	@Test
	@Timeout(value = 4, unit = TimeUnit.SECONDS)
	void initialRequest() throws Exception {
		Status status = mock(Status.class);
		when(status.getDeviceList()).thenReturn(mock(DeviceList.class));
		when(status.summaryHasChanged(0)).thenReturn(true);
		HttpRequest request = mock(HttpRequest.class);
		when(request.getMethod()).thenReturn(Method.GET);
		when(request.getPath()).thenReturn("/v3/poll");
		when(request.isAuthorised()).thenReturn(true);
		Controller controller = new FrontController(status, request);
		controller.processRequest();
		verify(request).sendHeaders(200, "Long Poll", "application/json");
		verify(request).close();
	}

	@Test
	@Timeout(value = 4, unit = TimeUnit.SECONDS)
	void pollUntilStatusChanges() throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getMethod()).thenReturn(Method.GET);
		when(request.getParam("hashcode")).thenReturn("123456");
		when(request.getPath()).thenReturn("/v3/poll");
		when(request.isAuthorised()).thenReturn(true);
		Status status = mock(Status.class);
		when(status.summaryHasChanged(anyInt())).thenReturn(false);
		when(status.getDeviceList()).thenReturn(mock(DeviceList.class));

		// Run the controller in a separate thread, so can test change of state
		Controller controller = new FrontController(status, request);
		Thread thread = new Thread(controller);
		thread.start();
		Thread.sleep(500);
		verify(request, never()).sendHeaders(anyInt(), anyString(), anyString());
		verify(request, never()).close();
		when(status.summaryHasChanged(123456)).thenReturn(true);
		verify(request, timeout(100).times(1)).sendHeaders(200, "Long Poll", "application/json");
		verify(request).close();
	}

	/**
	 * Tests that the long poll endpoint times out after given time
	 */
	@Test
	@Timeout(value = 4, unit = TimeUnit.SECONDS)
	void pollTimeout() throws Exception {
		HttpRequest request = mock(HttpRequest.class);
		when(request.getMethod()).thenReturn(Method.GET);
		when(request.getParam("hashcode")).thenReturn("123456");
		when(request.isAuthorised()).thenReturn(true);
		Status status = mock(Status.class);
		when(status.summaryHasChanged(anyInt())).thenReturn(false);
		when(status.getDeviceList()).thenReturn(mock(DeviceList.class));

		// Run the controller in a separate thread
		Controller controller = new LongPollControllerV3(status, request, 2, 30); // Call LongPollControllerV3 directly so timeout value can be configured
		Thread thread = new Thread(controller);
		thread.start();
		Thread.sleep(500);
		verify(request, never()).sendHeaders(anyInt(), anyString(), anyString());
		verify(request, never()).close();

		// Poll should return after 2 seconds
		verify(request, timeout(2000).times(1)).sendHeaders(200, "Long Poll", "application/json");
		verify(request).close();
	}

	@Test
	@Timeout(value = 2, unit = TimeUnit.SECONDS)
	void pollModifiesDeviceList() throws Exception {
		DeviceList deviceList = new DeviceList(mock(Loganne.class));
		Device device1 = deviceList.getDevice("device1");
		HttpRequest request = mock(HttpRequest.class);
		when(request.getMethod()).thenReturn(Method.GET);
		when(request.getParam("hashcode")).thenReturn("123456");
		when(request.removeParam("device")).thenReturn("device1");
		when(request.isAuthorised()).thenReturn(true);
		Status status = mock(Status.class);
		when(status.summaryHasChanged(anyInt())).thenReturn(true);
		when(status.getDeviceList()).thenReturn(deviceList);
		assertFalse(deviceList.isConnected(device1));

		// Run the controller in a separate thread, so can test change of state
		Controller controller = new LongPollControllerV3(status, request, 30, 1); // Call LongPollControllerV3 directly so tidyup value can be configured
		Thread thread = new Thread(controller);
		thread.start();
		verify(request, timeout(100).times(1)).sendHeaders(200, "Long Poll", "application/json");
		verify(request).close();

		// The device should stay in the device list for 1 second and then be removed.
		assertTrue(deviceList.isConnected(device1));
		Thread.sleep(1000);
		assertFalse(deviceList.isConnected(device1));
	}
	@Test
	@Timeout(value = 2, unit = TimeUnit.SECONDS)
	void RestoreDeviceListAfterInterrupt() throws Exception {
		DeviceList deviceList = new DeviceList(mock(Loganne.class));
		Device device1 = deviceList.getDevice("device1");
		HttpRequest request = mock(HttpRequest.class);
		when(request.getMethod()).thenReturn(Method.GET);
		when(request.getParam("hashcode")).thenReturn("123456");
		when(request.removeParam("device")).thenReturn("device1");
		when(request.isAuthorised()).thenReturn(true);
		Status status = mock(Status.class);
		when(status.summaryHasChanged(anyInt())).thenReturn(true);
		when(status.getDeviceList()).thenReturn(deviceList);
		assertFalse(deviceList.isConnected(device1));

		// Run the controller in a separate thread, so can test change of state
		Controller controller = new LongPollControllerV3(status, request, 30, 1); // Call LongPollControllerV3 directly so tidyup value can be configured
		Thread thread = new Thread(controller);
		thread.start();
		verify(request, timeout(100).times(1)).sendHeaders(200, "Long Poll", "application/json");
		verify(request).close();

		assertTrue(deviceList.isConnected(device1));
		thread.interrupt();
		Thread.sleep(100);
		assertFalse(deviceList.isConnected(device1));
	}

	@Test
	@Timeout(value = 4, unit = TimeUnit.SECONDS)
	void unsupportedMethod() throws Exception {
		Status status = mock(Status.class);
		when(status.getDeviceList()).thenReturn(mock(DeviceList.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getMethod()).thenReturn(Method.PUT);
		when(request.getPath()).thenReturn("/v3/poll");
		when(request.isAuthorised()).thenReturn(true);
		Controller controller = new FrontController(status, request);
		controller.processRequest();

		verify(request).notAllowed(Arrays.asList(Method.GET));
	}

	@Test
	void checkUnauthorisedResponse() throws Exception {
		Status status = mock(Status.class);
		when(status.getDeviceList()).thenReturn(mock(DeviceList.class));
		HttpRequest request = mock(HttpRequest.class);
		when(request.getMethod()).thenReturn(Method.GET);
		when(request.getPath()).thenReturn("/v3/poll");
		when(request.isAuthorised()).thenReturn(false);
		Controller controller = new FrontController(status, request);
		controller.run();
		verify(request).isAuthorised();
		verify(request).sendHeaders(401, "Unauthorized",  Map.of("Content-Type","text/plain","WWW-Authenticate","key"));
		verify(request).writeBody("Invalid API Key");
		verify(request).close();
	}
}