// Interface for classes which decide what logic to do based on a HTTP Request
interface Controller extends Runnable {
	public void processRequest() throws Exception;
}