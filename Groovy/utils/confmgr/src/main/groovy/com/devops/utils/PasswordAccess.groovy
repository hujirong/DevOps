package com.devops.utils

/**
 * Access password from config file
 * @author 
 *
 */
class PasswordAccess {
	static void main(String[] args) {
		if (args.length < 2) {
			println("Usage: password ConfigFile PasswordKey [KeyFile]")
			System.exit(0)
		}
		
		File confFile = new File(args[0])
		
		String passwordKey = args[1]
		
		File keyFile = null
		if (args.length > 2) {
			keyFile = args[2]
		}
		
		def conf = new ConfigFile(confFile)
		def password = conf.getConfig(passwordKey, true)
		println(password)
	}
}
