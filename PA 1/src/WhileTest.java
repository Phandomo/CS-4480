import java.net.Socket;
import java.util.ArrayList;


public class WhileTest
{
	public static void main(String[] args)
	{
		int i = 5;
		ArrayList<Socket> sockets = new ArrayList<Socket>();
		
		while (i > 0)
		{
			Socket newSocket = new Socket();
			sockets.add(newSocket);
			
			for (int j = 0; j < sockets.size(); j++)
			{
				if (newSocket.equals(sockets.get(j)))
				{
					System.out.println("The new socket equals the socket at index " + j);
				}
				else
				{
					System.out.print(".");
				}
			}
			
			i--;
		}
	}
}
