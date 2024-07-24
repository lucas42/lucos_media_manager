import java.io.IOException;
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
		System.err.println("WARNING: File Not found: ".concat(request.getPath()));
		request.sendHeaders(404, "Not Found", "text/plain");
		request.writeBody("File Not Found\n");
		request.close();
	}
}