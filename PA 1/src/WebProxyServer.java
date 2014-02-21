import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

/**
 * WebProxyServer.java - A simple caching HTTP Web Proxy Server
 * 
 * @author Rob Johansen
 */
public class WebProxyServer {
	
	
	/**
	 * The entry point into the application. A port
	 * number must be specified on the command line.
	 */
	public static void main(String[] args) {
		try {
			proxy(Integer.parseInt(args[0]));
		} catch (Exception e) {
			exitWithError(getUsage());
		}
	}
	
	/**
	 * Starts the proxy server on the specified port
	 * 
	 * @param port - an integer to use as the port number
	 */
	private static void proxy(int port) {
		ServerSocket welcomeSocket = null;
		ExecutorService executorService = null;
		ConcurrentMap<String, Lock> cacheLocks = new ConcurrentHashMap<>();
		
		try {
			welcomeSocket = new ServerSocket(port);
			executorService = Executors.newCachedThreadPool();
			
			while(true) {
				Socket clientSocket = welcomeSocket.accept();
				executorService.execute(new ProxyRequest(clientSocket, cacheLocks));
			}
		} catch (IOException e) {
			exitWithError("Error creating server socket: " + e.getMessage());
		} finally {
			try {
				if (welcomeSocket != null) {
					welcomeSocket.close();
				}
				
				if (executorService != null) {
					executorService.shutdown();
				}
			} catch (IOException e) {
				exitWithError("Error closing server socket: " + e.getMessage());
			}
		}
	}
	
	private static void exitWithError(String error) {
		System.out.println(error);
	    System.exit(1);
	}
	
	private static String getUsage() {
		return "Usage: java WebProxyServer <port>";
	}
}
