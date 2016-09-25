package com.devops.utils

import javax.crypto.Cipher

import org.apache.commons.codec.binary.Hex

class TextEncryptor {
	public static String encrypt(String value, File keyFile, String key) {
		if (isEncrypted(value)) {
			return value
		}
		
		def cipher = getCipher(true, keyFile, key);
		byte[] data = cipher.doFinal(value.getBytes())

		def result = "{OBF}" + Hex.encodeHex(data)
		return result
	}

	public static String decrypt(String value, File keyFile, String key) {
		if (!isEncrypted(value)) {
			return value
		}

		value = value.substring("{OBF}".length())
		byte[] encrypted = Hex.decodeHex(value.toCharArray())
		
		def cipher = getCipher(false, keyFile, key)
		byte[] original = cipher.doFinal(encrypted)

		def result = new String(original)
		return result
	}

	private static Cipher getCipher(boolean enc, File keyFile, String key) {
		def skeySpec = KeyManager.loadKey(keyFile, key)
		def cipher = Cipher.getInstance("AES")
		
		if (enc) {
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
		} else {
			cipher.init(Cipher.DECRYPT_MODE, skeySpec)
		}
		
		return cipher;
	}
	
	public static boolean isEncrypted(String value) {
		value.startsWith("{OBF}");
	}
}
