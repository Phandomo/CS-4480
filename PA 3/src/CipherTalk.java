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
	private static final String BOB_PUBLIC_KEY_FILENAME = "bobpublic.der";
	private static PrivateKey alicePrivateKey;
	private static PublicKey bobPublicKey;
	
	/* Message Hash */
	private static byte[] messageHash;
	
	/* Signature */
	private static byte[] signature;
	
	/* Payload */
	private static byte[] cipherText;
	
	public static void main(String[] args) throws FileNotFoundException,
												  IOException,
												  NoSuchAlgorithmException,
												  InvalidKeySpecException,
												  InvalidKeyException,
												  SignatureException,
												  NoSuchPaddingException,
												  IllegalBlockSizeException,
												  BadPaddingException {
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
		
		alicePrivateKey = loadPrivateKey(ALICE_PRIVATE_KEY_FILENAME);
		
		if (alicePrivateKey == null)
			error("Problem with Alice's private key...");
		
		vout("Signing the hash using Alice's private key");
		
		Signature rsa = Signature.getInstance("SHA1withRSA");
		rsa.initSign(alicePrivateKey);
		rsa.update(messageHash);
		signature = rsa.sign();
	}
	
	private static void encryptMessage() throws NoSuchAlgorithmException,
												NoSuchPaddingException,
												InvalidKeyException,
												IllegalBlockSizeException,
												BadPaddingException,
												FileNotFoundException,
												InvalidKeySpecException,
												IOException {
		byte[] messageParts = new byte[messageHash.length + signature.length + message.length];
		
		int hLength = messageHash.length;
		int sLength = signature.length;
		int mLength = message.length;
		
		System.arraycopy(messageHash, 0, messageParts, 0,       hLength);
		System.arraycopy(signature,   0, messageParts, hLength, sLength);
		System.arraycopy(message,     0, messageParts, hLength + sLength, mLength);
		
		vout("Generating a 3DES symmetric key");
		
		KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
		SecretKey tripleDesKey = keyGenerator.generateKey();
		
		vout("Encrypting hash, signature, and message to 3DES key");
		
		Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, tripleDesKey);
		cipherText = cipher.doFinal(messageParts);
		
		vout("Encrypting the 3DES key to Bob's key");
		
		bobPublicKey = loadPublicKey(BOB_PUBLIC_KEY_FILENAME);
		cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, bobPublicKey);
		byte[] symmetricKey = cipher.doFinal(tripleDesKey.getEncoded());
		
		print("Size of encrypted symmetric key in bytes: "+symmetricKey.length);
		
		// WYLO: Figure out how to store the size of the symmetric key in the first 4 bytes...
	}
	
	private static PublicKey loadPublicKey(String filename) throws FileNotFoundException,
																   IOException,
																   NoSuchAlgorithmException,
																   InvalidKeySpecException {
		File f = new File(filename);
		
		if (!f.exists())
			error("Public key is missing: "+filename);
		
		try(FileInputStream fis = new FileInputStream(f);
			DataInputStream dis = new DataInputStream(fis)) {
			byte[] keyBytes = new byte[(int)f.length()];
			dis.readFully(keyBytes);
			
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return kf.generatePublic(spec);
		}
	}
	
	private static PrivateKey loadPrivateKey(String filename) throws FileNotFoundException,
																	 IOException,
																	 NoSuchAlgorithmException,
																	 InvalidKeySpecException {
		File f = new File(filename);
		
		if (!f.exists())
			error("Private key is missing: "+filename);
		
		try(FileInputStream fis = new FileInputStream(f);
			DataInputStream dis = new DataInputStream(fis)) {
			byte[] keyBytes = new byte[(int)f.length()];
			dis.readFully(keyBytes);
			
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return kf.generatePrivate(spec);
		}
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
