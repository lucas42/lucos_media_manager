import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

import java.net.*;
import java.io.*;
import java.util.*;
class HttpRequestTest {

	void compareRequestResponse(Status status, String request, String responseSnippet) {
		try {
			Socket socket = mock(Socket.class);
			InetAddress mockedAddress = mock(InetAddress.class);
			when(mockedAddress.getHostName()).thenReturn("test.host");
			when(socket.getInetAddress()).thenReturn(mockedAddress);
			HttpRequest httpRequest = new HttpRequest(status, socket);
			InputStream input = new StringBufferInputStream(request);
			when(socket.getInputStream()).thenReturn(input);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			when(socket.getOutputStream()).thenReturn(output);
			httpRequest.processRequest();
			assertTrue(output.toString().contains(responseSnippet), "response snippet ("+responseSnippet+") not found in response:\n"+output.toString());
		} catch (Exception e) {
			fail(e);
		}
	}
	void compareRequestResponse(String request, String responseSnippet) {
		compareRequestResponse(null, request, responseSnippet);
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
		Status status = new Status(null, null);
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
		Status status = new Status(null, new DeviceList(null));
		compareRequestResponse(status, "POST /volume?volume=0.7 HTTP/1.1", "204");
		System.out.println(status.getVolume());
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "volume\":0.7");
	}
	@Test
	void devices() {
		Status status = new Status(null, new DeviceList(null));
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[]");
		compareRequestResponse(status, "POST /devices?uuid=46eca36b-2e4f-46bd-a756-249c45850cac HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Device 1\",\"isDefaultName\":true,\"isCurrent\":true,\"isConnected\":false}]");
		compareRequestResponse(status, "POST /devices?uuid=46eca36b-2e4f-46bd-a756-249c45850cac&name=Flying%20Fidget HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":false}]");
		compareRequestResponse(status, "POST /devices?uuid=3f03fae2-7a79-4ca1-9593-07c080f8402a&name=Jolly%20Jumper HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":false},{\"uuid\":\"3f03fae2-7a79-4ca1-9593-07c080f8402a\",\"name\":\"Jolly Jumper\",\"isDefaultName\":false,\"isCurrent\":false,\"isConnected\":false}]");
		compareRequestResponse(status, "POST /devices/current?uuid=3f03fae2-7a79-4ca1-9593-07c080f8402a HTTP/1.1", "204");
		compareRequestResponse(status, "GET /poll/summary HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":false,\"isConnected\":false},{\"uuid\":\"3f03fae2-7a79-4ca1-9593-07c080f8402a\",\"name\":\"Jolly Jumper\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":false}]");
		compareRequestResponse(status, "GET /poll/summary?device=46eca36b-2e4f-46bd-a756-249c45850cac HTTP/1.1", "\"devices\":[{\"uuid\":\"46eca36b-2e4f-46bd-a756-249c45850cac\",\"name\":\"Flying Fidget\",\"isDefaultName\":false,\"isCurrent\":false,\"isConnected\":true},{\"uuid\":\"3f03fae2-7a79-4ca1-9593-07c080f8402a\",\"name\":\"Jolly Jumper\",\"isDefaultName\":false,\"isCurrent\":true,\"isConnected\":false}]");
	}

}