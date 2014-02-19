import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


/**
 * ProxyRequest.java - Proxy reqeust designed for executionn in its own thread
 * 
 * @author Rob Johansen
 */
public class ProxyRequest implements Runnable
{
	Socket clientSocket;
	
	public ProxyRequest(Socket clientSocket)
	{
		this.clientSocket = clientSocket;
	}
	
	@Override
	public void run()
	{
		BufferedReader fromClient = null;
		Socket originSocket       = null;
		DataOutputStream toOrigin = null;
		BufferedReader fromOrigin = null;
		DataOutputStream toClient = null;
		
		try
		{
			// Read the request from the client
			fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			HttpRequest request = new HttpRequest(fromClient);
			
			// Send the request to the origin server
			originSocket = new Socket(request.getHost(), request.getPort());
			toOrigin     = new DataOutputStream(originSocket.getOutputStream());
			fromOrigin   = new BufferedReader(new InputStreamReader(originSocket.getInputStream()));
			
			toOrigin.writeBytes(request.toString());
			toOrigin.flush();
			
			// Receive the respone from the origin server and send it to the client
			String responseLine;
			String response = "";
			while ((responseLine = fromOrigin.readLine()) != null)
			{
				response += responseLine + "\r\n";
			}
			
			toClient = new DataOutputStream(clientSocket.getOutputStream());
			toClient.writeBytes(response);
			toClient.flush();
			clientSocket.close();
		}
		catch (IOException e)
		{
			showError("Transmission error: " + e.getMessage());
		}
		catch (HttpRequestException e)
		{
			showError("HTTP Error " + e.getStatusCode() + ": " + e.getMessage());
		}
		finally
		{
			try
			{
				if (fromClient   != null) fromClient.close();
				if (originSocket != null) originSocket.close();
				if (toOrigin     != null) toOrigin.close();
				if (fromOrigin   != null) fromOrigin.close();
			}
			catch (IOException e)
			{
				showError("Transmission error: " + e.getMessage());
			}
		}
	}
	
	private void showError(String error)
	{
		System.out.println(error);
	}
}
