import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
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
	private static final String CA_PUBLIC_KEY_FILENAME = "CApublic.der";
	private static final String BOB_CA_SIGNATURE = "bob_ca_signature";
	private static PrivateKey alicePrivateKey;
	private static PublicKey bobPublicKey;
	private static PublicKey caPublicKey;
	
	/* Message Hash */
	private static byte[] messageHash;
	
	/* Signature */
	private static byte[] signature;
	
	/* Payload */
	private static byte[] cipherText;
	private static byte[] payload;
	
	/* Entry Point */
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
		
		message = messageString.getBytes("UTF-8");
		
		vout("\n========================================\n"
		     + "    Starting CipherTalk Transmission\n"
		     + "========================================\n");
		
		vout("The message from Alice to Bob is: \n\n"+messageString+"\n");
		
		signMessage();
		encryptMessage();
		sendMessage();
		
		vout(""); // If output is verbose, add a new line at the very end
	}
	
	private static void signMessage() throws FileNotFoundException,
											 IOException,
											 NoSuchAlgorithmException,
											 InvalidKeySpecException,
											 InvalidKeyException,
											 SignatureException {
		vout("Creating a SHA-1 message digest");
		
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(message);
		messageHash = md.digest();
		
		printHex("The message digest", messageHash);
		
		alicePrivateKey = loadPrivateKey(ALICE_PRIVATE_KEY_FILENAME);
		
		printHex("Alice's private key", alicePrivateKey.getEncoded());
		
		if (alicePrivateKey == null)
			error("Problem with Alice's private key...");
		
		vout("Signing the message digest using Alice's private key");
		
		Signature rsa = Signature.getInstance("SHA1withRSA");
		rsa.initSign(alicePrivateKey);
		rsa.update(messageHash);
		signature = rsa.sign();
		
		printHex("Alice's signature of the message digest", signature);
	}
	
	private static void encryptMessage() throws NoSuchAlgorithmException,
												NoSuchPaddingException,
												InvalidKeyException,
												IllegalBlockSizeException,
												BadPaddingException,
												FileNotFoundException,
												InvalidKeySpecException,
												IOException,
												SignatureException {
		vout("Loading the CA's public key");
		caPublicKey = loadPublicKey(CA_PUBLIC_KEY_FILENAME);
		
		printHex("The CA's public key", caPublicKey.getEncoded());
		
		vout("Loading the CA's signature of Bob's public key");
		FileInputStream fis = new FileInputStream(BOB_CA_SIGNATURE);
		byte[] CAsignature = new byte[fis.available()]; 
		fis.read(CAsignature);
		fis.close();
		
		printHex("The CA's signature of Bob's public key", CAsignature);
		
		vout("Loading Bob's public key");
		bobPublicKey = loadPublicKey(BOB_PUBLIC_KEY_FILENAME);
		
		printHex("Bob's public key", bobPublicKey.getEncoded());
		
		vout("Verifying the CA signature on Bob's public key");
		Signature rsa = Signature.getInstance("SHA1withRSA");
		rsa.initVerify(caPublicKey);
		rsa.update(bobPublicKey.getEncoded());
		boolean verified = rsa.verify(CAsignature);
		
		if (verified) {
			vout("Bob's public key is verified by the CA");
		} else {
			vout("Bob's public key is not verified by the CA!");
			vout("Exiting due to suspicion of an impostor public key!");
			System.exit(1);
		}
		
		int hLength = messageHash.length;
		int sLength = signature.length;
		int mLength = message.length;
		
		byte[] messageParts = new byte[hLength + sLength + mLength];
		
		System.arraycopy(messageHash, 0, messageParts, 0,       hLength);
		System.arraycopy(signature,   0, messageParts, hLength, sLength);
		System.arraycopy(message,     0, messageParts, hLength + sLength, mLength);
		
		vout("Generating a 3DES symmetric key");
		
		KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
		SecretKey tripleDesKey = keyGenerator.generateKey();
		
		printHex("The unencrypted 3DES key", tripleDesKey.getEncoded());
		
		vout("Encrypting the hash, signature, and message to the 3DES key");
		
		Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, tripleDesKey);
		cipherText = cipher.doFinal(messageParts);
		
		printHex("The ciphertext", cipherText);
		
		vout("Encrypting the 3DES key to Bob's public key");
		
		cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, bobPublicKey);
		byte[] symmetricKey = cipher.doFinal(tripleDesKey.getEncoded());
		
		printHex("The encrypted 3DES key", symmetricKey);
		
		/*
		 * The first four bytes of the payload will be the size of the symmetric key.
		 * This will allow Bob's version of the program to follow a simple algorithm
		 * after receiving the payload:
		 * 
		 * 1. Read the first four bytes to determine the size of the symmetric key, N.
		 * 2. Read the next N bytes to obtain the encrypted symmetric key.
		 * 3. Read the remaining bytes to obtain the ciphertext.
		 */
		
		vout("Combining all the ciphertext bytes");
		
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(symmetricKey.length);
		byte[] symmetricKeySize = buffer.array();
		
		int kLength = symmetricKeySize.length;
		int tLength = symmetricKey.length;
		int cLength = cipherText.length;
		
		payload = new byte[kLength + tLength + cLength];
		
		System.arraycopy(symmetricKeySize, 0, payload, 0,       kLength);
		System.arraycopy(symmetricKey,     0, payload, kLength, tLength);
		System.arraycopy(cipherText,       0, payload, kLength + tLength, cLength);
	}
	
	private static void sendMessage() throws UnknownHostException,
											 IOException {
		vout("Sending the ciphertext to Bob...");
		Socket bobSocket = new Socket(address, port);
		DataOutputStream dos = new DataOutputStream(bobSocket.getOutputStream());
		
		dos.writeInt(payload.length);
		dos.write(payload);
		dos.flush();
		
		bobSocket.close();
		dos.close();
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
	
	private static void printHex(String item, byte[] bytes) {
		StringBuffer hexString = new StringBuffer();
		
		for (int i = 0; i < bytes.length; i++) {
			hexString.append(Integer.toHexString(0xFF & bytes[i]));
		}
		
		vout("\n"+item+" in hex is:\n"+hexString+"\n");
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
