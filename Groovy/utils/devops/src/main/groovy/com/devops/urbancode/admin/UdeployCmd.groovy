package com.devops.urbancode.admin

import groovyx.net.http.HTTPBuilder
import jline.console.UserInterruptException

import com.beust.jcommander.Parameter
import com.devops.urbancode.deploy.DeployRestAPI
import com.devops.utils.ConfigFile
import com.devops.urbancode.admin.UdeployUpdate

abstract class UdeployCmd {
	String PUBLIC_API = '/cli'
	String NONPUB_API = '/rest/deploy'
	String PROP_API = '/property/propSheet/'
	
	@Parameter(names = "-U", description = "UrabanCode User", required = true)
	String user
	
	@Parameter(names = "-P", description = "Password", required = false)
	String password
	
	@Parameter(names = "-CP", required = false, arity = 1,
		description = "Read password from conf file (key is <user_name>.password, for example fhou.password")
	boolean readConfPassword = false
	
	DeployRestAPI deployAPI
	ConfigFile conf
	HTTPBuilder httpBuilder
	String webUrl
	
	abstract void run();
	
	void init() {
		conf = UdeployUpdate.loadConf()

		// Initialise password
		if (readConfPassword) {
			password = conf.getConfig(user + ".password", true)
		} else {
			if (!password?.trim()) {
				// get user/password
				def reader = new jline.console.ConsoleReader()
				reader.setHandleUserInterrupt(true)
				
				try {
					println("Enter password:")
					password = reader.readLine(new Character('*' as char))
				} catch (UserInterruptException ex) {
					println()
					println("Interrupted program and exit.")
					System.exit(0)
				}
			}
		}
		
		deployAPI = new DeployRestAPI(conf)
		webUrl = deployAPI.webUrl
	}
	
	void copyParams(UdeployCmd udcmd) {
		conf = udcmd.conf
		deployAPI = udcmd.deployAPI
		httpBuilder = udcmd.httpBuilder
		webUrl = udcmd.webUrl
	}
	
	void process() {
		try {
			httpBuilder = deployAPI.login(user, password)
			run()
		} finally {
			if (httpBuilder != null) {
				deployAPI.logout(httpBuilder)
			}
		}
	}
}
