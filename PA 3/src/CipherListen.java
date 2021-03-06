import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class CipherListen {
	
	/* Command-Line Options */
	private static int     port = -1;
	private static Boolean verbose = false;
	
	/* Payload */
	private static byte[] payload;
	
	/* Keys */
	private static final String BOB_PRIVATE_KEY_FILENAME = "bobprivate.der";
	private static PrivateKey bobPrivateKey;
	private static final String ALICE_PUBLIC_KEY_FILENAME = "alicepublic.der";
	private static PublicKey alicePublicKey;
	
	/* Application Entry Point */
	public static void main(String[] args) {
		// Gather the arguments
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-p") && args.length >= i)
				try { port = Integer.parseInt(args[i+1]); } catch (NumberFormatException e) { error("Invalid port"); }
			if (args[i].equals("-v"))
				verbose = true;
			if (args[i].equals("-h"))
				help();
		}
		
		if (port == -1)
			error("Missing argument: -p <port>");
		
		vout("\n=============================\n"
			 + "    Starting CipherListen    \n"
			 + "=============================\n");
		
		listen();
		
		try {
			decryptMessage();
		} catch (Exception e) {
			error("There was a problem decrypting the message: "+e.getMessage());
		}
		
		vout(""); // If output is verbose, add a new line at the very end
	}
	
	private static void listen() {
		ServerSocket bobSocket = null;
		
		try {
			vout("Waiting for a secure message from Alice...");
			bobSocket = new ServerSocket(port);
			Socket aliceSocket = bobSocket.accept();
			
			DataInputStream dis = new DataInputStream(aliceSocket.getInputStream());
			int payloadLength = dis.readInt(); // The first four bytes are the length of the payload
			
			if (payloadLength > 0) {
				vout("Now receiving a secure message from Alice");
				payload = new byte[payloadLength];
				dis.readFully(payload);
			} else {
				error("Error: The message from Alice was empty.");
			}
		} catch (IOException e) {
			error("Error creating Bob's socket: " + e.getMessage());
		} finally {
			if (bobSocket != null) {
				try {
					bobSocket.close();
				} catch (IOException e) {
					error("Error closing Bob's socket: " + e.getMessage());
				}
			}
		}
	}
	
	private static void decryptMessage() throws FileNotFoundException,
												NoSuchAlgorithmException,
												InvalidKeySpecException,
												IOException,
												NoSuchPaddingException,
												InvalidKeyException,
												IllegalBlockSizeException,
												BadPaddingException,
												SignatureException {
		// The first four bytes of the payload are the size of the symmetric key in bytes
		byte[] symmetricKeySizeBytes = new byte[4];
		
		for (int i = 0; i < 4; i++) {
			symmetricKeySizeBytes[i] = payload[i];
		}
		
		int symmetricKeySize = ByteBuffer.wrap(symmetricKeySizeBytes).getInt();
		
		// Extract the encrypted symmetric key
		byte[] encryptedSymmetricKey = new byte[symmetricKeySize];
		
		for (int i = 4, j = 0; i < 4 + symmetricKeySize; i++, j++) {
			encryptedSymmetricKey[j] = payload[i];
		}
		
		printHex("The encrypted 3DES key", encryptedSymmetricKey);
		
		vout("Decrypting the 3DES key with Bob's private key");
		
		// Decrypt the symmetric key with Bob's private key
		bobPrivateKey = loadPrivateKey(BOB_PRIVATE_KEY_FILENAME);
		
		printHex("Bob's private key", bobPrivateKey.getEncoded());
		
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, bobPrivateKey);
		byte[] symmetricKey = cipher.doFinal(encryptedSymmetricKey);
		
		printHex("The decrypted 3DES key", symmetricKey);
		
		vout("Decrypting the ciphertext using the 3DES key");
		
		SecretKey tripleDesKey = new SecretKeySpec(symmetricKey, "DESede");
		cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, tripleDesKey);
		
		byte[] encryptedMessageParts = new byte[payload.length - symmetricKeySize - 4];
		
		// Get the encrypted parts of the message from the payload
		for (int i = 4 + symmetricKeySize, j = 0; i < payload.length; i++, j++) {
			encryptedMessageParts[j] = payload[i];
		}
		
		printHex("The ciphertext", encryptedMessageParts);
		
		byte[] plainText = cipher.doFinal(encryptedMessageParts);
		
		// Decrypt the message so the message digest can be verified
		byte[] message = new byte[plainText.length - 20 - 128];
		
		for (int i = 20 + 128, j = 0; i < plainText.length; i++, j++) {
			message[j] = plainText[i];
		}
		
		String messageText = new String(message, "UTF-8");
		
		// Compute the message digest (20 bytes)
		byte[] messageBytes = messageText.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(messageBytes);
		byte[] computedMessageHash = md.digest();
		
		byte[] receivedMessageHash = new byte[20];
		
		for (int i = 0; i < 20; i++) {
			receivedMessageHash[i] = plainText[i];
		}
		
		// Get and verify the signature (128 bytes, starting at offset 20)
		vout("Checking the signature of the message.");
		
		alicePublicKey = loadPublicKey(ALICE_PUBLIC_KEY_FILENAME);
		
		printHex("Alice's public key", alicePublicKey.getEncoded());
		
		byte[] signatureBytes = new byte[128];
		
		for (int i = 20, j = 0; i < 148; i++, j++) {
			signatureBytes[j] = plainText[i];
		}
		
		printHex("Alice's signature of the message digest", signatureBytes);
		
		Signature rsa = Signature.getInstance("SHA1withRSA");
		rsa.initVerify(alicePublicKey);
		rsa.update(receivedMessageHash);
		boolean validSignature = rsa.verify(signatureBytes);
		
		if (validSignature)
			vout("The signature is verified. This message originated from Alice.");
		else
			vout("Signature verification failed. This message did not come from Alice!");
		
		printHex("The message digest", computedMessageHash);
		
		vout("Checking the integrity of the message.");
		
		// Determine whether the hashes match
		if (Arrays.equals(computedMessageHash, receivedMessageHash)) {
			vout("The message digest is correct. This message was not altered in transit.");
			print("\nThe plaintext message from Alice is:\n\n"+messageText);
		} else {
			vout("The message digest is incorrect. The message was altered in transit!");
			print("\nThe altered message is:\n\n"+messageText);
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
		print("\nCipherListen v0.1 by Rob Johansen\n\n"
				+ "Usage: java CipherListen -p <port> [-v]\n\n"
				+ "Arguments:\n"
				+ "    -p <port>  The port number on which this computer will listen for a secure message from Alice.\n"
				+ "    -v         Display verbose output.\n"
				+ "    -h         Print this message and exit.\n");
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
