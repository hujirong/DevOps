package com.cppib.core.urbancode.test

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import com.cppib.coretech.devops.utils.ConfigFile
import com.cppib.coretech.devops.versions.UdeployUpdate


class UTestConf {
	static Log log = LogFactory.getLog(UTestConf.class)
	
	ConfigFile conf
	Binding confBinding
	
	String env = null
	String user

	UTestConf() {
		init()
	}
	
	void init() {
		env = System.getProperty('env')
		if (env == null) {
			usage()
			log.error('Please define property -Denv=[dev|prod]')
			System.exit(-1)
		}
		
		user = System.getProperty('user')
		if (user == null) {
			usage()
			log.error('Please define property -Duser=<user_name>')
			System.exit(-1)
		}

		// load UTestConfig file
		loadUTestConf()
		
		// load configuration
		conf = UdeployUpdate.loadConf()
	}
	
	void usage() {
		println 'Usage: mvn -test -Denv=<ENV> -Duser=<UrbanCode User>'
		println '    Where ENV can be dev, prod or local'
		println '        UrbanCode User is the user to login to urbancode'
		
	}
	
	void loadUTestConf() {
		def utestConfFile = this.getClass().getResource('/utest.conf').toURI()
		confBinding = new Binding()
		GroovyShell shell = new GroovyShell(confBinding)
		shell.evaluate(utestConfFile)
	}
	
	def getEnvProperty(String name) {
		return confBinding.getProperty("${env}_${name}")
	}
	
	String getBaseUrl() {
		return confBinding.getProperty("${env}_url")
	}
}
