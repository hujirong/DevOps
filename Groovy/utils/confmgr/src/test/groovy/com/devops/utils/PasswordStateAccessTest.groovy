package com.devops.utils

import java.io.File;

import org.junit.After;
import org.junit.Test

import com.devops.utils.ConfigFile;
import com.devops.utils.PasswordStateAccess;;

class PasswordStateAccessTest {
	File file = new File("build/tmp/conn.properties")
	File keyFile = new File("build/tmp/mktest.properties")
	
	@Test
	void testPasswordStateAccess() {
		file.text = '''
# urbancode deploy
urbancode.deploy.user = brian
urbancode.deploy.password_PasswordID = 5521
urbancode.deploy.password_APIKey = 3862c5df9ab4e7eb2122a69c534dae28
'''
		def pwdReturn = 'wonderful'
		
		PasswordStateAccess.metaClass.'static'.getPassword = { String passwordID, String apiKey ->
			return pwdReturn
		}
		
		def conf = new ConfigFile(file, keyFile)
		def password = conf.getConfig('urbancode.deploy.password', true)
	
		assert password == pwdReturn
	}
	
	@After
	void tearDown() {
		file.delete()
		keyFile.delete()
	}
}
