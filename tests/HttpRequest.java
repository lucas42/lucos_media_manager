import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

import java.net.*;
import java.io.*;
import java.util.*;
class HttpRequestTest {

	void compareRequestResponse(String request, String responseSnippet) {
		try {
			Socket socket = mock(Socket.class);
			InetAddress mockedAddress = mock(InetAddress.class);
			when(mockedAddress.getHostName()).thenReturn("test.host");
			when(socket.getInetAddress()).thenReturn(mockedAddress);
			HttpRequest httpRequest = new HttpRequest(socket);
			InputStream input = new StringBufferInputStream(request);
			when(socket.getInputStream()).thenReturn(input);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			when(socket.getOutputStream()).thenReturn(output);
			httpRequest.run();
			assertTrue(output.toString().contains(responseSnippet), "response snippet ("+responseSnippet+") not found in response:\n"+output.toString());
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	void notFound() {
		compareRequestResponse("GET /unknown HTTP/1.1\n", "404 File Not Found");
	}
	@Test
	void robots() {
		compareRequestResponse("Get /robots.txt HTTP/1.1\n", "Disallow:");
	}
	@Test
	void playPause() {
		Manager.setPlaying(true);
		compareRequestResponse("POST /pause HTTP/1.0\n", "204");
		assertFalse(Manager.getPlaying());
		compareRequestResponse("POST /pause HTTP/1.0\n", "204");
		assertFalse(Manager.getPlaying());
		compareRequestResponse("POST /play HTTP/1.0\n", "204");
		assertTrue(Manager.getPlaying());
		compareRequestResponse("POST /play HTTP/1.0\n", "204");
		assertTrue(Manager.getPlaying());
		compareRequestResponse("POST /playpause HTTP/1.0\n", "204");
		assertFalse(Manager.getPlaying());
		compareRequestResponse("POST /playpause HTTP/1.0\n", "204");
		assertTrue(Manager.getPlaying());
	}
	@Test
	void control() {
		Manager.setPlaying(true);
		compareRequestResponse("POST /control?isPlaying=false&volume=0.7 HTTP/1.1", "204");

		// This actually sets the volume as a string, which I don't think it should do...
		compareRequestResponse("GET /poll/summary HTTP/1.1", "volume\":\"0.7\"");
		assertTrue(Manager.getPlaying());
	}

}