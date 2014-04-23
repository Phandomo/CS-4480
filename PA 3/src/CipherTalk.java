
public class CipherTalk {
	
	private static String  address;
	private static String  message;
	private static Boolean verbose = false;
	
	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-a") && args.length >= i)
				address = args[i+1];
			if (args[i].equals("-m") && args.length >= i)
				message = args[i+1];
			if (args[i].equals("-v"))
				verbose = true;
			if (args[i].equals("-h"))
				help();
		}
		
		if (address == null) {
			print("\nMissing argument: -a <address>\n");
			System.exit(1);
		}
		
		if (message == null) {
			print("\nMissing argument: -m <message>\n");
			System.exit(1);
		}
		
		
	}
	
	private static void help() {
		print("\nCipherTalk v0.1 by Rob Johansen\n\n"
				+ "Usage: java CipherTalk -a <address> -m <message> [-v]\n\n"
				+ "Arguments:\n"
				+ "    -a <address>  The IP address of Bob's computer.\n"
				+ "    -m <message>  The message to sign, encrypt, and send to Bob.\n"
				+ "    -v            Display verbose output.\n"
				+ "    -h            Print this message and exit.\n");
		System.exit(0);
	}
	
	private static void print(String message) {
		System.out.println(message);
	}
	
}
