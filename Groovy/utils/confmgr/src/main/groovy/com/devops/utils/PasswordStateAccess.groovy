package com.devops.utils

import groovyx.net.http.RESTClient

/**
 * Access password from PasswordState
 * @author 
 *
 */
class PasswordStateAccess {
	static final String PASSWORDSTATE_URL = 'https://passwordstate.otpp.ca/'

	static String getPassword(String passwordID, String apiKey, String pwdStateUrl = PASSWORDSTATE_URL) {
		if (System.getProperty("PWD_TRACE")) {
			println("PasswordState get passwordID=$passwordID, APIKey=$apiKey")
		}
		
		def client = new RESTClient(pwdStateUrl)
		client.ignoreSSLIssues()
		
		def	resp = client.get(path : "api/passwords/$passwordID", query : [apikey:apiKey])
		
		assert resp.status == 200
		return resp.data.Password[0]
	}	
}
