import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import java.net.* ;
import java.io.* ;

class LoganneTest {

	@Test
	void postEvent() throws Exception {

		ServerSocket serverSocket = new ServerSocket(7999);

		Loganne loganne = new Loganne("lucos_media_test", "http://localhost:7999");
		loganne.post("TestType", "A little message");

		// Accept and ignore the first connection - it's a dud
		serverSocket.accept();
		Socket socket = serverSocket.accept();
		InputStream is = socket.getInputStream();
		String rawRequest = "";
		for (int nextByte = is.read(); nextByte > -1; nextByte = is.read()) {
			rawRequest += (char) nextByte;
			if ((char) nextByte == '}') break;
		}
		String[] lines = rawRequest.split("\r\n");

		// Send simple response, to prevent errors being logged
		OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream(), "UTF8");
		osw.write("HTTP/1.1 202 Accepted\r\n");
		osw.close();
		assertEquals("POST /events HTTP/1.1", lines[0]);
		assertEquals("{\"source\":\"lucos_media_test\",\"type\":\"TestType\",\"humanReadable\":\"A little message\"}", lines[lines.length-1]);

	}

}