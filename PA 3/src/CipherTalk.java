import java.io.*;
import java.security.*;
import java.security.spec.*;

import javax.crypto.*;

public class CipherTalk {
	
	/* Command-Line Options */
	private static String  address;
	private static int     port = -1;
	private static String  messageString;
	private static byte[]  message;
	private static Boolean verbose = false;
	
	/* Keys */
	private static final String ALICE_PRIVATE_KEY_FILENAME = "aliceprivate.der";
	private static PrivateKey alicePrivateKey;
	private static byte[] symmetricKey;
	
	/* Message Hash */
	private static byte[] messageHash;
	
	/* Signature */
	private static byte[] signature;
	
	public static void main(String[] args) throws FileNotFoundException,
												  IOException,
												  NoSuchAlgorithmException,
												  InvalidKeySpecException,
												  InvalidKeyException,
												  SignatureException {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-a") && args.length >= i)
				address = args[i+1];
			if (args[i].equals("-p") && args.length >= i)
				try { port = Integer.parseInt(args[i+1]); } catch (NumberFormatException e) { error("Invalid port"); }
			if (args[i].equals("-m") && args.length >= i)
				messageString = args[i+1];
			if (args[i].equals("-v"))
				verbose = true;
			if (args[i].equals("-h"))
				help();
		}
		
		if (address == null)
			error("Missing argument: -a <address>");
		
		if (port == -1)
			error("Missing argument: -p <port>");
		
		if (messageString == null || messageString.length() == 0)
			error("Missing argument: -m <message>");
		
		message = messageString.getBytes();
		
		vout("\n========================================\n"
		     + "    Starting CipherTalk Transmission\n"
		     + "========================================\n");
		
		signMessage();
		encryptMessage();
		
		vout(""); // If output is verbose, add a new line at the very end
	}
	
	private static void signMessage() throws FileNotFoundException,
											 IOException,
											 NoSuchAlgorithmException,
											 InvalidKeySpecException,
											 InvalidKeyException,
											 SignatureException {
		vout("Creating a hash of the message");
		
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(message);
		messageHash = md.digest();
		
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
		
		if (alicePrivateKey == null)
			error("Problem with Alice's private key...");
		
		vout("Signing the hash using Alice's private key");
		
		Signature rsa = Signature.getInstance("SHA1withRSA");
		rsa.initSign(alicePrivateKey);
		rsa.update(messageHash);
		signature = rsa.sign();
	}
	
	private static void encryptMessage() throws NoSuchAlgorithmException {
		// WYLO: Combine the hash, signature, and message so they can be encrypted with a 3DES symmetric key
		
		
		vout("Generating a 3DES symmetric key");
		
		KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
		SecretKey tripleDesKey = keyGenerator.generateKey();
		symmetricKey = tripleDesKey.getEncoded();
		
		
	}
	
	private static void vout(String text) {
		if (verbose)
			print(text);
	}
	
	private static void help() {
		print("\nCipherTalk v0.1 by Rob Johansen\n\n"
				+ "Usage: java CipherTalk -a <address> -p <port> -m <message> [-v]\n\n"
				+ "Arguments:\n"
				+ "    -a <address>  The IP address of Bob's computer.\n"
				+ "    -p <port>     The port number on which Bob's computer is listening.\n"
				+ "    -m <message>  The message to sign, encrypt, and send to Bob.\n"
				+ "    -v            Display verbose output.\n"
				+ "    -h            Print this message and exit.\n");
		System.exit(0);
	}
	
	private static void error(String text) {
		print("\n"+text+"\n");
		System.exit(1);
	}
	
	private static void print(String text) {
		System.out.println(text);
	}
	
}
