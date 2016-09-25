package com.devops.utils

import org.junit.After
import org.junit.Test

import com.devops.utils.KeyManager;;

class KeyManagerTest {
	static File keyFile = new File("build/tmp/mktest.key")
	
	@Test
	void testKeyManager() {
		def key0 = "uuaa22d#!"
		
		def key1 = KeyManager.loadKey(keyFile, key0)
		def key2 = KeyManager.loadKey(keyFile, key0)
		
		assert key1 == key2
	}
	
	@After
	void tearDown() {
		keyFile.delete()
	}
}