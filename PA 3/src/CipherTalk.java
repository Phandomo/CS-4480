import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class CipherTalk {
	
	/* Command-Line Options */
	private static String  address;
	private static String  message;
	private static Boolean verbose = false;
	
	/* Keys */
	private static final String ALICE_PRIVATE_KEY_FILENAME = "aliceprivate.der";
	private static PrivateKey alicePrivateKey;
	
	public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
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
		
		if (address == null)
			error("Missing argument: -a <address>");
		
		if (message == null)
			error("Missing argument: -m <message>");
		
		signMessage();
	}
	
	private static void signMessage() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		File f = new File(ALICE_PRIVATE_KEY_FILENAME);
		
		if (!f.exists())
			error("Alice's private key is missing: "+ALICE_PRIVATE_KEY_FILENAME);
		
		try(FileInputStream fis = new FileInputStream(f);
			DataInputStream dis = new DataInputStream(fis)) {
			byte[] keyBytes = new byte[(int)f.length()];
			dis.readFully(keyBytes);
			
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			alicePrivateKey = kf.generatePrivate(spec);
		}
		
		if (alicePrivateKey == null) error("Problem with Alice's private key...");
		else print("Woohoo!");
		
		message = "-----BEGIN CipherTalk SIGNED MESSAGE-----\n" + message + "\n";
		
//		String signature
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
	
	private static void error(String message) {
		print("\n"+message+"\n");
		System.exit(1);
	}
	
	private static void print(String message) {
		System.out.println(message);
	}
	
}
