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
					request.sendHeaders(204, "Changed");
					request.close();
				} else if (request.getData().toLowerCase().equals("false")) {
					status.setPlaying(false);
					request.sendHeaders(204, "Changed");
					request.close();
				} else {
					request.sendHeaders(400, "Not Changed", "text/plain");
					request.writeBody("Unknown value \"".concat(request.getData()).concat("\""));
					request.close();
				}
			} else {
				request.notAllowed(Arrays.asList(Method.PUT));
			}
		} else if (request.getPath().equals("/v3/volume")) {
			if (request.getMethod().equals(Method.PUT)) {
				try{
					float value = Float.parseFloat(request.getData());
					if (!(value <= 1.0)) {
						request.sendHeaders(400, "Not Changed", "text/plain");
						request.writeBody("Volume must not be greater than 1.0");
						request.close();
					} else if (!(value >= 0.0)) {
						request.sendHeaders(400, "Not Changed", "text/plain");
						request.writeBody("Volume must not be less than 0.0");
						request.close();
					} else {
						status.setVolume(value);
						request.sendHeaders(204, "Changed");
						request.close();
					}
				} catch (NullPointerException e) {
					request.sendHeaders(400, "Not Changed", "text/plain");
					request.writeBody("Request body must be set to value for volume");
					request.close();
				} catch (NumberFormatException e) {
					request.sendHeaders(400, "Not Changed", "text/plain");
					request.writeBody("Volume must be a number");
					request.close();
				}
			} else {
				request.notAllowed(Arrays.asList(Method.PUT));
			}

		} else if (request.getPath().startsWith("/v3/device-names/")) {
			if (request.getMethod().equals(Method.PUT)) {
				String uuid = request.getPath().replace("/v3/device-names/", "");
				status.getDeviceList().updateDevice(uuid, request.getData());
				request.sendHeaders(204, "Changed");
				request.close();
			} else {
				request.notAllowed(Arrays.asList(Method.PUT));
			}
		} else if (request.getPath().equals("/v3/current-device")) {
			if (request.getMethod().equals(Method.PUT)) {
				status.getDeviceList().setCurrent(request.getData());
				request.sendHeaders(204, "Changed");
				request.close();
			} else {
				request.notAllowed(Arrays.asList(Method.PUT));
			}
		} else {
			System.err.println("WARNING: File Not found: ".concat(request.getPath()));
			request.sendHeaders(404, "Not Found", "text/plain");
			request.writeBody("File Not Found");
			request.close();
		}
	}
}