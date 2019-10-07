package com.devops.urbancode.admin

import groovy.util.logging.Slf4j;
import groovyx.net.http.HTTPBuilder
import jline.console.UserInterruptException

import com.beust.jcommander.Parameter
import com.devops.urbancode.deploy.DeployRestAPI
import com.devops.utils.ConfigFile
import com.devops.urbancode.admin.UdeployUpdate

/**
 * Copied from UdeployCmd
 * Using account urbancode.deploy.user if -U is not specified
 * @author fhou
 *
 */
@Slf4j
abstract class UdeployCmd2 {
	static String PUBLIC_API = '/cli'
	static NONPUB_API = '/rest/deploy'
	static PROP_API = '/property/propSheet/'
	
	@Parameter(names = "-U", description = "UrabanCode User, if not specified use urbancode.deploy.user", required = false)
	String user
	
	@Parameter(names = "-P", description = "Password, read from conf file if not specified: <user_name>.password if -U is used, otherwise urbancode.deploy.password", required = false)
	String password
	
	DeployRestAPI deployAPI
	ConfigFile conf
	HTTPBuilder httpBuilder
	String webUrl
	
	abstract void run();
	
	void init() {
		conf = UdeployUpdate.loadConf()

		// Initialise password
		if (!user?.trim()) {
			println "Find login user urbancode.deploy.user..."
			user = conf.getConfig('urbancode.deploy.user')
			password = conf.getConfig('urbancode.deploy.password', true)
		} else {
			if (!password?.trim()) {
				// try to find password from conf
				password = conf.getConfig(user + '.password', true)
				
				if (password == null) {
					println "Cannot find password ${user}.password in conf file."
					
					// get password
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
		}
		
		println "Connect to UCD with user $user"
		deployAPI = new DeployRestAPI(conf)
		webUrl = deployAPI.webUrl
	}
	
	void process() {
		try {
			httpBuilder = deployAPI.login(user, password)
			
			// set default failure handler
			httpBuilder.handler.failure = { resp ->
				def req = resp.context['http.request']
				
				log.error("HTTP request failed: ${req.method} ${req.URI}")
				log.error("     response status ${resp.status} ${resp.statusLine}")
				
				throw new Exception("HTTP failure")
			}
			
			run()
		} finally {
			if (httpBuilder != null) {
				deployAPI.logout(httpBuilder)
			}
		}
	}

}
