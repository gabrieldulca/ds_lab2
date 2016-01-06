package util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Please note that this class is not needed for Lab 1, but can later be
 * used in Lab 2.
 * 
 * Provides security provider related utility methods.
 */
public final class SecurityUtils {

	private SecurityUtils() {
	}

	/**
	 * Registers the {@link BouncyCastleProvider} as the primary security
	 * provider if necessary.
	 */
	public static synchronized void registerBouncyCastle() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.insertProviderAt(new BouncyCastleProvider(), 0);
		}
	}
	
	

	public static String generateChallenge() {
		byte[] challenge = new byte[32];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(challenge);
		String encodedChallenge = new String (Base64.encode(challenge));
		return encodedChallenge;
	}
	
	public static byte[] generateIV() {
		byte[] iv = new byte[16];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(iv);
		return iv;
	}
	
	public static String encodeIV(byte[] iv){
		return new String (Base64.encode(iv));
	}
	
	public static String encodeSessionKey(byte[] sessionKey){
		return new String (Base64.encode(sessionKey));
	}
	
	/**
	 * Encrypt message with RSA-Publickey and encode with base64
	 * 
	 * @param message
	 * @param publicKey
	 * @return
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] encryptMessageRSA(byte[] message, PublicKey publicKey)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		// init cipher:
		Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		// encrypt and encode:
		byte[] cipherMessage = cipher.doFinal(message);
		return Base64.encode(cipherMessage);
	}

	/**
	 * Decrypt base64 encoded message with RSA-Privatekey.
	 * 
	 * @param cipherMessage
	 * @param privateKey
	 * @return
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public static byte[] decryptMessageRSA(byte[] cipherMessage, PrivateKey privateKey)
			throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException {
		// init cipher:
		Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		// encrypt and encode:
		byte[] message = cipher.doFinal(Base64.decode(cipherMessage));
		return message;
	}
	
	/**
	 * Encrypt message with AES-Key and encode with base64.
	 * 
	 * @param message
	 * @param secretKey
	 * @param ivParameter
	 * @return
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 */
	public static byte[] encryptMessageAES(byte[] message, SecretKey secretKey, byte[] ivParameter)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(ivParameter));
		byte[] cipherMessage = cipher.doFinal(message);
		return Base64.encode(cipherMessage);
	}

	/**
	 * Decrypt base64 encoded message with AES-Key.
	 * 
	 * @param cipherMessage
	 * @param secretKey
	 * @param ivParameter
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidAlgorithmParameterException
	 */
	public static byte[] decryptMessageAES(byte[] cipherMessage, SecretKey secretKey,
			byte[] ivParameter) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException {
		Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivParameter));
		byte[] message = cipher.doFinal(Base64.decode(cipherMessage));
		return message;
	}

	public static SecretKey generateKeyAES() {
		KeyGenerator generator;
		try {
			generator = KeyGenerator.getInstance("AES");
		}
		catch (NoSuchAlgorithmException e) {
			// should never happen! precondition: valid jre installation?!
			e.printStackTrace();
			return null;
		}
		// KEYSIZE is in bits
		generator.init(256);
		SecretKey key = generator.generateKey();
		return key;
	}
	
	/**
	 * Decode base64 encoded String.
	 * 
	 * @param string
	 * @return
	 */
	public static byte[] decode(String string) {
		return Base64.decode(string.getBytes());
	}
}
