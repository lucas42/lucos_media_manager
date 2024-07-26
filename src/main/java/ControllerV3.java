import java.io.IOException;
import java.util.Arrays;
class ControllerV3 implements Controller {
	private Status status;
	private HttpRequest request;

	public ControllerV3(Status status, HttpRequest request) {
		this.status = status;
		this.request = request;
	}
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			System.err.println("ERROR: Unknown Error (ControllerV3, host:"+request.getHostName()+"):");
			e.printStackTrace();
		}
	}
	public void processRequest() throws IOException {
		if (request.getPath().equals("/v3/is-playing")) {
			if (request.getMethod().equals(Method.PUT)) {
				if (request.getData().toLowerCase().equals("true")) {
					status.setPlaying(true);
					request.sendHeaders(204, "No Content");
					request.close();
				} else if (request.getData().toLowerCase().equals("false")) {
					status.setPlaying(false);
					request.sendHeaders(204, "No Content");
					request.close();
				} else {
					request.sendHeaders(400, "Bad Request", "text/plain");
					request.writeBody("Unknown value \"".concat(request.getData()).concat("\"\n"));
					request.close();
				}
			} else {
				request.notAllowed(Arrays.asList(Method.PUT));
			}
		} else {
			System.err.println("WARNING: File Not found: ".concat(request.getPath()));
			request.sendHeaders(404, "Not Found", "text/plain");
			request.writeBody("File Not Found\n");
			request.close();
		}
	}
}