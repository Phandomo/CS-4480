import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class GenSig {
	
	public static void main(String[] args) throws NoSuchAlgorithmException,
												  FileNotFoundException,
												  InvalidKeySpecException,
												  IOException,
												  InvalidKeyException,
												  SignatureException {
		PublicKey bobPublic  = loadPublicKey("bobpublic.der");
		PrivateKey CAprivate = loadPrivateKey("CAprivate.der");
		
		Signature rsa = Signature.getInstance("SHA1withRSA");
		rsa.initSign(CAprivate);
		rsa.update(bobPublic.getEncoded());
		byte[] signature = rsa.sign();
		
		FileOutputStream fos = new FileOutputStream("bob_ca_signature");
		fos.write(signature);
		fos.close();
	}
	
	private static PublicKey loadPublicKey(String filename) throws FileNotFoundException,
																   IOException,
																   NoSuchAlgorithmException,
																   InvalidKeySpecException {
		File f = new File(filename);
		
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
		
		try(FileInputStream fis = new FileInputStream(f);
			DataInputStream dis = new DataInputStream(fis)) {
			byte[] keyBytes = new byte[(int)f.length()];
			dis.readFully(keyBytes);
			
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return kf.generatePrivate(spec);
		}
	}
	
}
