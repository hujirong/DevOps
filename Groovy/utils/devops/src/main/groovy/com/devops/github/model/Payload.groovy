package com.devops.github.model

import com.devops.urbancode.deploy.model.Component

class Payload {
	String ref
	String before
	// has to list all properties!  
	//groovy.lang.MissingPropertyException: No such property: before for class: com.devops.github.model.Payload
	String after
	Map repository
	Map sender
	Map pusher
	String created
	String deleted
	String forced
	String base_ref		
	// commits
	List commits = []
	String compare
	Commit head_commit
	
	public List getCommits() {		
		return commits
	}
}
