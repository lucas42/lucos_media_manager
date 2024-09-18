import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.Test;

import java.net.*;
import java.io.*;
import java.util.*;
class HttpRequestTest {

	@Test
	void SimpleGetRequest() throws IOException {
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("GET /simple-path HTTP/1.1\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);

		assertEquals("test.host", request.getHostName());
		request.readFromSocket();
		assertEquals("/simple-path", request.getPath());
		assertEquals(Method.GET, request.getMethod());

		request.sendHeaders(200, "OK", "text/plain");
		request.writeBody("All Good");
		request.close();
		assertEquals("HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nServer: lucos\r\nContent-Type: text/plain; charset=utf-8\r\n\r\nAll Good\r\n", output.toString());
	}

	@Test
	void PostRequestWithBody() throws IOException {
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("POST /data-body?replace=yes HTTP/1.1\r\nContent-type:application/json\r\nContent-Length: 20\r\n\r\n {\"object-count\":7} \r\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);

		request.readFromSocket();
		assertEquals("/data-body", request.getPath());
		assertEquals(Method.POST, request.getMethod());
		assertEquals("yes", request.getParam("replace"));
		assertEquals("yes", request.getParam("replace"));
		assertEquals("{\"object-count\":7}", request.getData());

		request.sendHeaders(204, "No Content", "text/plain");
		request.close();
		assertEquals("HTTP/1.1 204 No Content\r\nAccess-Control-Allow-Origin: *\r\nServer: lucos\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n", output.toString());
	}

	@Test
	void RequestNotAllowed() throws IOException {
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("DELETE /read-only-endpoint?force HTTP/1.1\r\n\r\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);

		request.readFromSocket();
		assertEquals("/read-only-endpoint", request.getPath());
		assertEquals(Method.DELETE, request.getMethod());
		assertEquals("true", request.getParam("force"));
		assertEquals("", request.getData());

		request.notAllowed(Arrays.asList(Method.GET, Method.POST));
		assertEquals("HTTP/1.1 405 Method Not Allowed\r\nAccess-Control-Allow-Origin: *\r\nServer: lucos\r\nAllow: GET, POST\r\n\r\n", output.toString());
	}

	@Test
	void HeadRequestIgnoresResponseBody() throws IOException {
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("HEAD /contentpage?style=override HTTP/1.1\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);
		request.readFromSocket();
		assertEquals("/contentpage", request.getPath());
		assertEquals("override", request.getParam("style", "default"));
		assertEquals("default", request.getParam("font", "default"));
		assertEquals(Method.HEAD, request.getMethod());

		request.sendHeaders(200, "OK");
		request.writeBody("Lots of content here.  But it should never be output because it's a HEAD request");
		request.close();
		assertEquals("HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nServer: lucos\r\n\r\n", output.toString());
	}

	@Test
	void redirect() throws IOException {
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("PUT /data-body?style=override HTTP/1.1\r\nContent-type:application/json\r\nContent-Length: 20\r\nX-Custom-Header\r\n\r\n {\"object-count\":9} \r\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);

		request.readFromSocket();
		assertEquals("/data-body", request.getPath());
		assertEquals("{\"object-count\":9}", request.getData());
		assertEquals(Method.PUT, request.getMethod());
		assertEquals("override", request.removeParam("style"));
		assertEquals(null, request.removeParam("style"));

		request.redirect("/newpage");
		assertEquals("HTTP/1.1 302 Redirect\r\nAccess-Control-Allow-Origin: *\r\nServer: lucos\r\nLocation: /newpage\r\n\r\n", output.toString());

	}
	@Test
	void unknownMethod() throws IOException {
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("BLAHBLAH /simple-path HTTP/1.1\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);

		assertEquals("test.host", request.getHostName());
		request.readFromSocket();
		assertEquals("/simple-path", request.getPath());
		assertEquals(Method.UNKNOWN, request.getMethod());

		request.notAllowed(Arrays.asList(Method.PUT, Method.DELETE));
		assertEquals("HTTP/1.1 405 Method Not Allowed\r\nAccess-Control-Allow-Origin: *\r\nServer: lucos\r\nAllow: PUT, DELETE\r\n\r\n", output.toString());

	}
	@Test
	void optionsRequest() throws IOException {
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("OPTIONS /simple-path HTTP/1.1\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);

		assertEquals("test.host", request.getHostName());
		request.readFromSocket();
		assertEquals("/simple-path", request.getPath());
		assertEquals(Method.OPTIONS, request.getMethod());

		request.notAllowed(Arrays.asList(Method.PUT, Method.DELETE));

		// Order of some headers is non-deterministic, so check for contains, rather than exact match
		assertTrue(output.toString().contains("HTTP/1.1 204 No Content\r\nAccess-Control-Allow-Origin: *\r\nServer: lucos\r\n"));
		assertTrue(output.toString().contains("Access-Control-Allow-Headers: Authorization\r\n"));
		assertTrue(output.toString().contains("Access-Control-Allow-Methods: PUT, DELETE\r\n"));

	}

	@Test
	void authenticatedRequest() throws IOException {
		HttpRequest.setClientKeys("apikeys:lucos_test:production=L37sXhRBod7u5uxSFkUH;lucos_test2:development=JXfLoaaFX349FU8RYZgL");
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("POST /authenticatedPath HTTP/1.1\r\nAuthorization: Key L37sXhRBod7u5uxSFkUH\n\r\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);
		request.readFromSocket();
		assertTrue(request.isAuthorised());
	}

	@Test
	void missingAuthorisation() throws IOException {
		HttpRequest.setClientKeys("apikeys:lucos_test:production=L37sXhRBod7u5uxSFkUH;lucos_test2:development=JXfLoaaFX349FU8RYZgL");
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("POST /authenticatedPath HTTP/1.1\r\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);
		request.readFromSocket();
		assertFalse(request.isAuthorised());
	}

	@Test
	void unrecognisedAuthorisationScheme() throws IOException {
		HttpRequest.setClientKeys("apikeys:lucos_test:production=L37sXhRBod7u5uxSFkUH;lucos_test2:development=JXfLoaaFX349FU8RYZgL");
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("POST /authenticatedPath HTTP/1.1\r\nAuthorization: SpecialSchemer\n\r\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);
		request.readFromSocket();
		assertFalse(request.isAuthorised());
	}

	@Test
	void unauthenticatedRequest() throws IOException {
		HttpRequest.setClientKeys("apikeys:lucos_test:production=L37sXhRBod7u5uxSFkUH;lucos_test2:development=JXfLoaaFX349FU8RYZgL");
		Socket socket = mock(Socket.class);
		InetAddress mockedAddress = mock(InetAddress.class);
		when(mockedAddress.getHostName()).thenReturn("test.host");
		when(socket.getInetAddress()).thenReturn(mockedAddress);
		InputStream input = new StringBufferInputStream("POST /authenticatedPath HTTP/1.1\r\nAuthorization: Key TX9nfU85CnZAlzDfmt3Qr\n\r\n");
		when(socket.getInputStream()).thenReturn(input);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(output);

		HttpRequest request = new HttpRequest(socket);
		request.readFromSocket();
		assertFalse(request.isAuthorised());
	}
}