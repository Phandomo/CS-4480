import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;


/**
 * ProxyRequest.java - Proxy request designed for execution in its own thread
 * 
 * @author Rob Johansen
 */
public class ProxyRequest implements Runnable {
	
	Socket clientSocket;
	ConcurrentMap<String, Lock> cacheLocks;
	final String cacheFileName = "cached";
	
	public ProxyRequest(Socket clientSocket, ConcurrentMap<String, Lock> cacheLocks) {
		this.clientSocket = clientSocket;
		this.cacheLocks = cacheLocks;
	}
	
	@Override
	public void run() {
		BufferedReader fromClient = null;
		Socket originSocket = null;
		DataOutputStream toOrigin = null;
		BufferedReader fromOriginText = null;
		BufferedImage fromOriginImage = null;
		
		try {
			// Read the request from the client
			fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			HttpRequest request = new HttpRequest(fromClient);
			
			// Hash the URI portion of the request 
			String uriHash = hashRequestUri(request.getUri());
			
			// Prepare the cache file (for either reading or writing)
			Path cachedFile = FileSystems.getDefault().getPath(uriHash, cacheFileName);
			
			// Prepare a couple String variables for reading the response
			String responseLine;
			StringBuilder response = new StringBuilder();
			
			// If necessary, associate a new Lock with the hash of this request
			Lock cacheLock = cacheLocks.putIfAbsent(uriHash, new ReentrantLock());
			
			try {
				if (cacheLock == null) {
					System.out.println("No response for this request in cache: " + request.getUri());
					
					// Acquire the lock for synchronization
					cacheLock = cacheLocks.get(uriHash);
					cacheLock.lock();
					
					// GET http://www.cs.utah.edu/~kobus/simple.html HTTP/1.0
					// GET http://www.bridgetjohansen.com/images/bridget.jpg HTTP/1.0
					
					// Send the request to the origin server
					if (request.isImage()) {
						fromOriginImage = ImageIO.read(new URL(request.getUri())); 
					} else {
						
						originSocket = new Socket(request.getHost(), request.getPort());
						toOrigin = new DataOutputStream(originSocket.getOutputStream());
						toOrigin.writeBytes(request.toString());
						toOrigin.flush();
						
						// Receive the text respone from the origin server
						fromOriginText = new BufferedReader(new InputStreamReader(originSocket.getInputStream()));
						while ((responseLine = fromOriginText.readLine()) != null) {
							response.append(responseLine + "\r\n");
						}
					}
					
					// If necessary, create the directory that will store this cached response
					Path hashDirectory = FileSystems.getDefault().getPath(uriHash);
					
					if (!Files.exists(hashDirectory)) {
						Files.createDirectory(hashDirectory);
					}
					
					// If necessary, delete the old cached file (from a previous run of the server)
					if (Files.exists(cachedFile)) {
						Files.delete(cachedFile);
					}
					
					// Cache the response and send it to the client
					if (request.isImage()) {
						String extension = request.getImageExtension();
						ImageIO.write(fromOriginImage, extension, new File("./" + uriHash + "/" + cacheFileName + "." + extension));
						sendImageResponseToClient(clientSocket.getOutputStream(), fromOriginImage, extension);
					} else {
						try (BufferedWriter writer = Files.newBufferedWriter(cachedFile, Charset.forName(Charset.defaultCharset().toString()))) {
							System.out.println("Writing response to cache: ./" + uriHash + "/" + cacheFileName);
							writer.write(response.toString(), 0, response.length());
						} catch (IOException e) {
							showError("Problem writing response to cache: " + e.getMessage());
						}
						
						sendTextResponseToClient(clientSocket.getOutputStream(), response.toString());
					}
				} else {
					System.out.println("Retrieving request from cache: " + request.getUri());
					
					cacheLock.lock();
					
					// Read the response from the cache
					if (request.isImage()) {
						// TODO
					} else {
						try (BufferedReader reader = Files.newBufferedReader(cachedFile, Charset.forName(Charset.defaultCharset().toString()))) {
							while ((responseLine = reader.readLine()) != null) {
								response.append(responseLine + "\r\n");
							}
						}
						
						// Send the cached response to the client
						sendTextResponseToClient(clientSocket.getOutputStream(), response.toString());
					}
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
				clientSocket.close();
				if (fromClient != null) fromClient.close();
				if (originSocket != null) originSocket.close();
				if (toOrigin != null) toOrigin.close();
				if (fromOriginText != null) fromOriginText.close();
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
	
	private void sendImageResponseToClient(OutputStream stream, BufferedImage image, String extension) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, extension, baos);
		baos.flush();
		byte[] bytes = baos.toByteArray();
		stream.write(bytes);
		stream.flush();
		baos.close();
	}
	
	private void sendTextResponseToClient(OutputStream stream, String response) throws IOException {
		DataOutputStream toClient = new DataOutputStream(stream);
		toClient.writeBytes(response);
		toClient.flush();
		toClient.close();
	}

	private void showError(String error) {
		System.out.println(error);
	}
}
