package com.devops.urbancode.deploy.model

class Component extends UrbancodeObject {
	String template = ''
	
	// FULL or INCREMENTAL
	String defaultVersionType = 'FULL'
	
	String repositoryName
	String groupId
	String artifactId
}
