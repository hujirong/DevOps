package com.devops.urbancode.deploy.model

import java.util.List;

class UrbancodeObject {
	static List types = ['DEV', 'QA', 'UAT', 'PROD']
	static String UNDEFINE = 'TODO'
	
	String id
	String name
	String description = ''

	// TeamSecurity list
	List tsecs = []
	
	List tags
	
	def addTeams(List teams) {
		teams.each {
			TeamSecurity ts = new TeamSecurity(teamLabel: it.teamLabel, resourceRoleLabel: it.resourceRoleLabel)
			tsecs.add(ts)
		}
	}
}
