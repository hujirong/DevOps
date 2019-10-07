package com.devops.urbancode.deploy.model

/**
 * UrbanCode deploy record 
 * @author fhou
 *
 */
class DeployRecord {
	String requestId

	String bmcNumber
	String jiraNumber
		
	String appName
	String envName
	String envType
	
	String date
	String duration
	
	String user
	String status
	
	List serverNames
	List dbNames
}
