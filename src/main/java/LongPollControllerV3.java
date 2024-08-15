import java.io.IOException;
import java.util.Arrays;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
/**
 * Handles long polling requests
 */
class LongPollControllerV3 extends Controller {
	int POLL_TIMEOUT; // Number of seconds before the Long Poll should return, even if no changes have happened
	int DEVICE_CONNECTION_TIDYUP; // Number of seconds after the request ends before tidying up the request in the device list
	public LongPollControllerV3(Status status, HttpRequest request, int POLL_TIMEOUT, int DEVICE_CONNECTION_TIDYUP) {
		super(status, request);
		this.POLL_TIMEOUT = POLL_TIMEOUT;
		this.DEVICE_CONNECTION_TIDYUP = DEVICE_CONNECTION_TIDYUP;
	}

	// Overwrites the standard Controller run method to handle tidying up devices afterwards
	public void run() {
		super.run();

		try {
			// Wait before marking this connection as closed, to give the client time to re-establish a long poll
			Thread.sleep(DEVICE_CONNECTION_TIDYUP * 1000);
		} catch (InterruptedException e) {}
		status.getDeviceList().closeConnection(request);
	}

	protected void processRequest() throws IOException, InterruptedException {
		if (request.getMethod().equals(Method.GET)) {
			status.getDeviceList().openConnection(request);
			long startTime = System.nanoTime();
			int hashcode;
			try {
				hashcode = Integer.parseInt(request.getParam("hashcode"));
			} catch (NumberFormatException e) {
				hashcode = 0;
			}
			while (true) {
				// Only respond when a change has occurred, or the request has taken over 30 seconds
				if (status.summaryHasChanged(hashcode) || (System.nanoTime() - startTime) > (POLL_TIMEOUT * 1000000000L)) {
					request.sendHeaders(200, "Long Poll", "application/json");
					Gson gson = CustomGson.get(status);
					request.writeBody(gson.toJson(status.getSummary()));
					request.close();
					return;
				}
				Thread.sleep(1);
			}
		} else {
			request.notAllowed(Arrays.asList(Method.GET));
		}
	}
}