package com.devops.urbancode.admin

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.devops.urbancode.deploy.model.UrbancodeObject
/*
 * Manage group (ReadOnly) members 
 *
 */
@Slf4j
@Parameters(commandDescription = "Manage group")
class GroupManager extends UdeployCmd {
	static String GROUP_READONLY = "ReadOnly"
	
	@Parameter(names = "-URO", description = "Update Readonly group (requires admin priviledge)", required = false)
	boolean updateReadonly = false
	
	String groupName
	String id = null
	
	List members = []
	
	def getGroupId() {
		def groups = httpBuilder.get(path: '/security/group')
		groups.each {
			if (groupName == it.name) {
				id = it.id
				return true
			}
		}
		
		if (id == null) {
			throw new Exception("Cannot find group ${groupName}")
		}
	}
	
	def getGroupMembers() {
		def items = httpBuilder.get(path: "/security/group/${id}/members")
		items.each {
			members.add(it.id)
		}
	}
	
	def addUsers(List users) {
		getGroupId()
		getGroupMembers()
		
		users.each { user ->
			if (!members.contains(user.id)) {
				log.info("Add user ${user.name} to group ${groupName}")
				
				def url = webUrl + "/security/group/${id}/members/${user.id}"
				
				httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
					uri.path = url
						
					response.failure = { resp, reader ->
						log.error(reader.text)
						throw new Exception("Add user to group failed")
					}
				}
		
			}
		}
	}

	def updateReadOnlyGroup() {
		groupName = GROUP_READONLY
		
		log.info("Update group members ${groupName}...")

		// get all users
		List users = []
		
		def items = httpBuilder.get(path: '/security/user', query: [name: '*'])
		items.each {
			def u = new UrbancodeObject(id: it.id, name: it.name)
			users.add(u)
		}

		// add users to group
		addUsers(users)
	}
	
	def clearReadOnlyGroup() {
		log.info("Clear members in ReadOnly group")
		throw new Exception("Not implemented!")	
	}
	
	@Override
	public void run() {
		if (updateReadonly) {
			updateReadOnlyGroup()
		}
	}
}
