import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * ProxyRequest.java - Proxy request designed for execution in its own thread
 * 
 * @author Rob Johansen
 */
public class ProxyRequest implements Runnable {
	
	Socket clientSocket;
	ConcurrentMap<String, Lock> cacheLocks;
	
	public ProxyRequest(Socket clientSocket, ConcurrentMap<String, Lock> cacheLocks) {
		this.clientSocket = clientSocket;
		this.cacheLocks = cacheLocks;
	}
	
	@Override
	public void run() {
		BufferedReader fromClient = null;
		Socket originSocket       = null;
		DataOutputStream toOrigin = null;
		BufferedReader fromOrigin = null;
		DataOutputStream toClient = null;
		
		try {
			// Read the request from the client
			fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			HttpRequest request = new HttpRequest(fromClient);
			
			// Hash the URI portion of the request 
			String uriHash = hashRequestUri(request.getUri());
			
			Lock cacheLock = cacheLocks.get(uriHash);
			
			try {
				if (cacheLock == null) {
					System.out.println("Request not in cache: " + request.getUri());
					
					// Associate a new Lock with the hash of this request
					cacheLock = cacheLocks.putIfAbsent(uriHash, new ReentrantLock());
					cacheLock.lock();
					
					// Send the request to the origin server
					originSocket = new Socket(request.getHost(), request.getPort());
					toOrigin     = new DataOutputStream(originSocket.getOutputStream());
					fromOrigin   = new BufferedReader(new InputStreamReader(originSocket.getInputStream()));
					
					toOrigin.writeBytes(request.toString());
					toOrigin.flush();
					
					// Receive the respone from the origin server
					String responseLine;
					String response = "";
					
					while ((responseLine = fromOrigin.readLine()) != null) {
						response += responseLine + "\r\n";
					}
					
					// TODO: Write the response to disk
					
					
					// Send the response to the client
					toClient = new DataOutputStream(clientSocket.getOutputStream());
					toClient.writeBytes(response);
					toClient.flush();
					clientSocket.close();
				} else {
					// TODO: Read the response from the cache
					// TODO: Send the cached response to the client
				}
			} finally {
				cacheLock.unlock();
			}
		} catch (IOException e) {
			showError("Transmission error: " + e.getMessage());
		} catch (HttpRequestException e) {
			showError("HTTP error " + e.getStatusCode() + ": " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			showError("Cache error: " + e.getMessage());
		} finally {
			try {
				if (fromClient   != null) fromClient.close();
				if (originSocket != null) originSocket.close();
				if (toOrigin     != null) toOrigin.close();
				if (fromOrigin   != null) fromOrigin.close();
			} catch (IOException e) {
				showError("Transmission error: " + e.getMessage());
			}
		}
	}
	
	private String hashRequestUri(String uri) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(uri.getBytes());
		byte byteData[] = md.digest();
		
		StringBuilder uriHash = new StringBuilder();
		
		for (int i = 0; i < byteData.length; i++) {
			String hex = Integer.toHexString(0xff & byteData[i]);
			
			if (hex.length() == 1)
				uriHash.append('0');
			
			uriHash.append(hex);
		}
		
		return uriHash.toString();
	}
	
	private void sendResponseToClient() {
		
	}

	private void showError(String error) {
		System.out.println(error);
	}
}
