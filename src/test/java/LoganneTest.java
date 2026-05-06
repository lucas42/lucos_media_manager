import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import java.net.*;
import java.io.*;
import java.util.Map;

class LoganneTest {

	private String capturePostedBody(int port, Runnable postAction) throws Exception {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			postAction.run();

			// Accept and ignore the first connection - it's a dud
			serverSocket.accept().close();
			try (Socket socket = serverSocket.accept()) {
				InputStream is = socket.getInputStream();
				String rawRequest = "";
				for (int nextByte = is.read(); nextByte > -1; nextByte = is.read()) {
					rawRequest += (char) nextByte;
					if ((char) nextByte == '}')
						break;
				}

				// Send simple response, to prevent errors being logged
				OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream(), "UTF8");
				osw.write("HTTP/1.1 202 Accepted\r\n");
				osw.close();

				String[] lines = rawRequest.split("\r\n");
				assertEquals("POST /events HTTP/1.1", lines[0]);
				return lines[lines.length - 1];
			}
		}
	}

	@Test
	void postEvent() throws Exception {
		String body = capturePostedBody(7999, () -> {
			try {
				Loganne loganne = new Loganne("lucos_media_test", "http://localhost:7999/events");
				loganne.post("TestType", "A little message");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		assertEquals(
				"{\"source\":\"lucos_media_test\",\"type\":\"TestType\",\"humanReadable\":\"A little message\"}",
				body);
	}

	@Test
	void postEventWithStructuredFields() throws Exception {
		String body = capturePostedBody(7998, () -> {
			try {
				Loganne loganne = new Loganne("lucos_media_test", "http://localhost:7998/events");
				loganne.post("collectionSwitch", "Switched to collection Robots", Map.of(
						"slug", "robots",
						"name", "Robots",
						"collectionSize", 42
				));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		// Verify required base fields
		assertTrue(body.contains("\"source\":\"lucos_media_test\""), "body should contain source");
		assertTrue(body.contains("\"type\":\"collectionSwitch\""), "body should contain type");
		assertTrue(body.contains("\"humanReadable\":\"Switched to collection Robots\""), "body should contain humanReadable");
		// Verify structured fields are included
		assertTrue(body.contains("\"slug\":\"robots\""), "body should contain slug");
		assertTrue(body.contains("\"name\":\"Robots\""), "body should contain name");
		assertTrue(body.contains("\"collectionSize\":42"), "body should contain collectionSize");
	}

}