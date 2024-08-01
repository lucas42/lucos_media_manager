public abstract class Controller implements Runnable {
	protected Status status;
	protected HttpRequest request;

	public Controller(Status status, HttpRequest request) {
		this.status = status;
		this.request = request;
	}
	abstract protected void processRequest() throws Exception;
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			System.err.println("ERROR: Unknown Error (Class:"+this.getClass().getSimpleName()+", Host:"+request.getHostName()+"):");
			e.printStackTrace();
		}
	}
}