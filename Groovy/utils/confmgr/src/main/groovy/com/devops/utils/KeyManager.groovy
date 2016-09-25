package com.devops.utils

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Hex


class KeyManager {
	static SecretKey loadKey(File file, String keyName) {
		def props = load(file)

		def keyValue = props.getProperty(keyName)		
		if (keyValue == null) {
			keyValue = generateKey()
			saveKey(file, keyName, keyValue)
			return keyValue
		}
		
		def decoded = Hex.decodeHex(keyValue.toCharArray())
		new SecretKeySpec(decoded, "AES")
	}

	static private Properties load(File file) {
		Properties props = new Properties()
		
		if (!file.exists()) {
			file.parentFile.mkdirs()
			return props
		}

		file.withInputStream {
			props.load(it)
		}
		
		return props;
	}
	
	static private generateKey() {
		def keyGenerator = KeyGenerator.getInstance("AES")
		keyGenerator.init(128)
		keyGenerator.generateKey()
	}
	
	static private saveKey(file, keyName, keyValue) {
		char[] hex = Hex.encodeHex(keyValue.getEncoded())
		def lsep = System.getProperty("line.separator")
		file << "$keyName = $hex$lsep"
		
		def ant = new AntBuilder()
		ant.chmod(file: file, perm: "600")
	}
}