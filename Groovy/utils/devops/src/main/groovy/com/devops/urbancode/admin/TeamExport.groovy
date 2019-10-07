package com.devops.urbancode.admin

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.Method
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.devops.urbancode.deploy.model.Application
import com.devops.urbancode.deploy.model.Component
import com.devops.urbancode.deploy.model.Environment
import com.devops.urbancode.deploy.model.Resource
import com.devops.urbancode.deploy.model.Team
import com.devops.urbancode.deploy.model.TeamBuilder

@Slf4j
@Parameters(commandDescription = "Export team resources")
class TeamExport extends UdeployCmd {
	static String CUSTOM_ARTIFACT_ID = "custom/artifactId"
	static String CUSTOM_GROUP_ID = "custom/groupId"
	static String CUSTOM_REPO_NAME = "custom/repositoryName"
	
	@Parameter(names = "-F", description = "Output file", required = true)
	String fileName
	
	@Parameter(names = "-T", description = "Team name", required = true)
	String teamName

	Team team
	
	boolean isMyTeam(List teams, String teamName) {
		boolean myteam = false
		
		teams.find { team ->
			if (team.teamLabel == teamName) {
				myteam = true
				return true
			}
		}
		
		return myteam
	}
	
	@Override
	public void run() {
		query()
		output()
	}
	
	def query() {
		team = new Team(name: teamName)
		
		queryApplications()
		queryComponents()
		queryResources()
	}
	
	def queryComponents() {
		log.info("Query team components ...")
		
		def li = httpBuilder.get(path: PUBLIC_API + '/component') 
		li.each {
			queryComponent(it.name)
		}
	}
	
	def findComponentProperty(String name, comp0) {
		def val = ''
		
		comp0.properties.find {
			if (it.name == name) {
				val = it.value
				return true
			}
		}
		
		return val
	}
	
	def isMyComponent(compInfo) {
		if (isMyTeam(compInfo.extendedSecurity.teams, teamName)) {
			return true
		}
		
		// check if component is in the application
		def myComp = false
		
		for (Application app : team.applications) {
			for (Component comp : app.comps) {
				if (comp.name == compInfo.name) {
					return true
				}
			}
		}
		
		return false
	}
	
	def queryComponent(compName) {
		def info = httpBuilder.get(path: PUBLIC_API + '/component/info', 
			query: [component: compName])
		
		// check if it is my component
		if (!isMyComponent(info)) {
			return
		}
		
		Component comp = new Component(id: info.id, name: info.name, description: info.description, defaultVersionType: info.defaultVersionType)
		
		// template name: API doesn't return it now
		/*
		if (cwt.template) {
			comp1.template = cwt.template.name
		}
		*/
		
		// artifactory info
		comp.repositoryName = findComponentProperty(CUSTOM_REPO_NAME, info)
		comp.artifactId = findComponentProperty(CUSTOM_ARTIFACT_ID, info)
		comp.groupId = findComponentProperty(CUSTOM_GROUP_ID, info)
		comp.tags = info.tags
		comp.addTeams(info.extendedSecurity.teams)
		
		team.components.add(comp)
	}
	
	def queryResources() {
		log.info("Query team resources ...")
		def li = httpBuilder.get(path: PUBLIC_API + '/resource')
		li.each {
			Resource res = queryResource(it.id, true)
			if (res != null) {
				team.resources.add(res)
			}
		}
	}

	Resource queryResource(String id, boolean isTop) {
		def info = httpBuilder.get(path: PUBLIC_API + '/resource/info',
			query: [resource: id])
		
		if (isTop) {
			def myres = isMyTeam(info.extendedSecurity.teams, teamName)
			if (!myres) {
				return null
			}
		}
		
		Resource res = new Resource(name: info.name, id: info.id, description: info.description)
		res.tags = info.tags
		res.inheritTeam = info.inheritTeam
		
		res.addTeams(info.extendedSecurity.teams)
		
		// type
		res.tsecs.find {
			if (it.teamLabel == teamName) {
				res.makeType(it.resourceRoleLabel)
				return true 
			}
		}
		
		// secure
		if (info.tags.contains('Secure')) {
			res.secure = true
		}
		
		// category
		// yes, udeploy calls it type
		//
		res.makeCategory(info)
		
		// impersonation
		if (info.impersonationUser?.trim()) {
			res.impersonationUser = info.impersonationUser
			res.impersonationUseSudo = info.impersonationUseSudo
			res.impersonationForce = info.impersonationForce
		}

		// query ITAM
		if (isTop) {
			queryResourceITAM(res)
		}
		
		// query child resources
		def childList = httpBuilder.get(path: PUBLIC_API + '/resource',
			query: [parent: res.id])
		
		
		childList.each {
			Resource child = queryResource(it.id, false)
			if (child != null) {
				res.addChild(child)
			}
		}

		return res
	}	
	
	def queryResourceITAM(Resource res) {
		def info = httpBuilder.get(path: PUBLIC_API + '/resource/getProperties',
			query: [resource: res.id])

		info.each { it ->
			if (it.name == 'ITAMAppID') {
				res.ITAMAppID = it.value
			} else if (it.name == 'ITAMTeamName') {
				res.ITAMTeamName = it.value
			}
		}
	}
	
	def queryApplications() {
		log.info("Query team applications ...")
		def li = httpBuilder.get(path: PUBLIC_API + '/application')
		li.each {
			queryApplication(it.name)
		}
	}
	
	def queryApplication(String appName) {
		def info = httpBuilder.get(path: PUBLIC_API + '/application/info',
			query: [application: appName])
		
		// check if it is my application
		boolean myapp = isMyTeam(info.extendedSecurity.teams, teamName)
		if (!myapp) {
			return
		}

		Application app = new Application(name: info.name, id: info.id, description: info.description)
		app.tags = info.tags
		app.addTeams(info.extendedSecurity.teams)
		
		team.applications.add(app)
		
		queryEnvironments(app)
		queryApplicationComponents(app)
	}
	
	def queryEnvironments(Application app) {
		def envList = httpBuilder.get(path: PUBLIC_API + '/application/environmentsInApplication',
			query: [application: app.name])

		envList.each {
			Environment env = queryEnvironement(app, it.name)
			app.envs.add(env)
		}
		
	}
	
	Environment queryEnvironement(Application app, String envName) {
		def info = httpBuilder.get(path: PUBLIC_API + '/environment/info',
			query: [environment: envName, application: app.name])
		
		Environment env = new Environment(name: info.name, id: info.id, description: info.description)
		env.addTeams(info.extendedSecurity.teams)
		
		// make type
		env.tsecs.find {
			if (it.teamLabel == teamName) {
				env.makeType(it.resourceRoleLabel)
				return true
			}
		}

		// artifactoryUrl
		/*
		httpBuilder.request(Method.GET, ContentType.TEXT) { req ->
			uri.path = PUBLIC_API + '/environment/getProperty'
			uri.query = [application: app.name, environment: envName, name: 'artifactoryUrl']
			
			response.success = { resp, reader ->
				env.artifactoryUrl = reader.text
			}
				 
			response.'404' = { resp, reader ->
				log.debug("Application ${app.name} environment ${envName} property artifactoryUrl is not defined")
			}
		}
		*/
		
		// base resources
		def baseResourceList = httpBuilder.get(path: PUBLIC_API + '/environment/getBaseResources',
			query: [application: app.name, environment: envName])
		
		baseResourceList.each {
			Resource res = new Resource(name: it.name, id: it.id, path: it.path)
			env.resources.add(res)
		}
		
		return env
	}
	
	def queryApplicationComponents(Application app) {
		def li = httpBuilder.get(path: PUBLIC_API + '/application/componentsInApplication',
			query: [application: app.name])
		
		li.each {
			Component comp = new Component(id: it.id, name: it.name)
			app.comps.add(comp)
		}
	}
	
	def output() {
		log.info("Output team resources to file $fileName ...")
		
		FileWriter fw = new FileWriter(new File(fileName))
		TeamBuilder builder = new TeamBuilder(fw)
		team.output(builder)
		
		fw.close()
	}
}
