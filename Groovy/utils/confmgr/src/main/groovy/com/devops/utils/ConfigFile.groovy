package com.devops.utils

/**
 * Properties with password encryption
 * 
 * - If the password value is clear text, encrypt it when get it
 * - If the password value is setup with PasswordState, use the PasswordState API to get it.
 * 
 * @author 
 *
 */
class ConfigFile {
	final String PASSWORD_ID = "_PasswordID"
	final String API_KEY = "_APIKey"
	final String KEY_FILE = ".devops/.keymgr.properties"
	File keyFile
	File configFile
	
	Properties props = new Properties()
	
	ConfigFile(File configFile, File keyFile = null) {
		this.configFile = configFile
		configFile.withInputStream {
			props.load(it)
		}

		initKeyFile(keyFile)
	}
	
	private void initKeyFile(File keyFile) {
		if (keyFile != null) {
			this.keyFile = keyFile
		} else {
			this.keyFile = new File(System.getProperty('user.home'), KEY_FILE)
		}
	}
	
	String getConfig(String key, boolean isPassword = false) {
		return getConfig(key, null, isPassword)
	}
	
	String getConfig(String key, String defaultValue, boolean isPassword = false) {
		if (!isPassword) {
			return props.getProperty(key)?.trim()
		}
		
		def passwordId = getPasswordID(key)
		def apiKey = getAPIKey(key)
		
		if (passwordId != null || apiKey != null) {
			return getPasswordFromPasswordState(passwordId, apiKey)?.trim()
		} else {
			return getPassword(key)?.trim()
		}
	}

	private getKeyName(key) {
		return configFile.name + '_' + key
	}
	
	private getPasswordID(String key) {
		props.getProperty(key + PASSWORD_ID)
	}

	private getAPIKey(String key) {
		def apiKeyKey = key + API_KEY 
		def apiKey = props.getProperty(apiKeyKey)
		if (apiKey == null) {
			return apiKey
		}
		
		def keyName = getKeyName(apiKeyKey)
		
		if (TextEncryptor.isEncrypted(apiKey)) {
			apiKey = TextEncryptor.decrypt(apiKey, keyFile, keyName)
			return apiKey
		}

		// APIKey is clear text, encrypt it
		def encryptedAPIKey = TextEncryptor.encrypt(apiKey, keyFile, keyName)
		updateConfig(apiKeyKey, encryptedAPIKey)
		
		return apiKey
	}
	
	private String getPassword(key) {
		// key name is used in key manager
		def keyName = getKeyName(key)
		
		def value = props.getProperty(key)?.trim()
		
		if (value == null || value.length() == 0) {
			return value
		}
		
		if (TextEncryptor.isEncrypted(value)) {
			// decrypt pwd
			def pwd = TextEncryptor.decrypt(value, keyFile, keyName)
			return pwd
		}
		
		// value is clear text, encrypt it
		def encryptedPwd = TextEncryptor.encrypt(value, keyFile, keyName)
		updateConfig(key, encryptedPwd)
		
		return value
	}
	
	private String getPasswordFromPasswordState(passwordId, apiKey) {
		assert (passwordId != null && apiKey != null) : "Need both passwordID and APIKey"
		PasswordStateAccess.getPassword(passwordId, apiKey)
	}
	
	// update value in propsFile
	private updateConfig(key, value) {
		props.setProperty(key, value)
		
		def ant = new AntBuilder()
		
		ant.replaceregexp(file:configFile.getPath(),
			match:"${key}( *)=(.*)",
			replace:"${key} = ${value}",
			byline:true)
	}
}
