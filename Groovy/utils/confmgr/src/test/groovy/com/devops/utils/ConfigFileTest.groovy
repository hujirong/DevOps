package com.devops.utils

import org.junit.After
import org.junit.Test

import com.devops.utils.ConfigFile;;

class ConfigFileTest {
	static File keyFile = new File("build/tmp/mktest.key")
	static File configFile = new File("build/tmp/mktest.properties")
	
	String userKey = "urbancode.deploy.user"
	String userValue = ""
	String pwdKey = "urbancode.deploy.password"
	String pwdValue = "test2221!!\$\$"
	
	void setupPassword() {
		configFile.text = 
"""
$userKey = $userValue	
$pwdKey = $pwdValue
"""
	}
	
	void setupPasswordState() {
		configFile.text = 
"""
"""
	}

	@Test
	void testPassword() {
		setupPassword()
		
		def config = new ConfigFile(configFile, keyFile)
		
		def user = config.getConfig(userKey)
		assert user == userValue
		
		def pwd1 = config.getConfig(pwdKey, true)
		assert pwd1 == pwdValue
		
		config = new ConfigFile(configFile, keyFile)
		def pwd2 = config.getConfig(pwdKey, true)
		assert pwd1 == pwd2
		
		println "$user:$pwd2".bytes.encodeBase64().toString()
	}

	@Test
	void testWhiteSpace() {
		configFile.text = 
"""
$userKey =   $userValue		     	
$pwdKey =    $pwdValue     
"""
		
		def config = new ConfigFile(configFile, keyFile)
		
		def user = config.getConfig(userKey)
		assert user == userValue
		
		def pwd1 = config.getConfig(pwdKey, true)
		assert pwd1 == pwdValue
		
		config = new ConfigFile(configFile, keyFile)
		def pwd2 = config.getConfig(pwdKey, true)
		assert pwd1 == pwd2
	}
	
	@Test
	void testNullKeyValue() {
		configFile.text = 
"""
urbancode.deploy.proxyHost =	
urbancode.deploy.proxyUser =	
"""

		def config = new ConfigFile(configFile, keyFile)
		
		// no encryption
		String proxyHost = config.getConfig("urbancode.deploy.proxyHost")
		assert proxyHost.length() == 0
		
		proxyHost = config.getConfig("urbancode.deploy.proxyHost2")
		assert proxyHost == null

		// encryption
		String proxyUser = config.getConfig("urbancode.deploy.proxyUser", true)
		assert proxyUser.length() == 0
		
		proxyUser = config.getConfig("urbancode.deploy.proxyUser2", true)
		assert proxyUser == null
	}
	
	@After
	void tearDown() {
		keyFile.delete()
		configFile.delete()
	}
}
