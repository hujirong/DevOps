package com.devops.utils

import org.junit.After
import org.junit.Test

import com.devops.utils.TextEncryptor;;

class TextEncryptorTest {
	static File keyFile = new File("build/tmp/mktest.key")
	
	@Test
	void testTextEncryptor() {
		def str = "Long long ago, there was a monkey"
		def key = "my.key"
		
		def str1 = TextEncryptor.encrypt(str, keyFile, key)
		def str2 = TextEncryptor.decrypt(str1, keyFile, key)
		assert str == str2

		// encrypt same string, but with different key		
		key = "your.key"
		str1 = TextEncryptor.encrypt(str, keyFile, key)
		str2 = TextEncryptor.decrypt(str1, keyFile, key)
		
		assert str == str2
	}
	
	@After
	void tearDown() {
		keyFile.delete()
	}
}