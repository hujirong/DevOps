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

/**
 * Import/update team resource (include component, component template, process, application and resource)
 *
 * @author fhou
 *
 */
@Slf4j
@Parameters(commandDescription = "Import/update team resources")
class TeamImport extends UdeployCmd {
	static List TAG_COLORS = [
		'FFFFFF', 'CFE2FF', 'A3EAFF', '83F0FF', 'ACFFFF', '2EF32E', 'CFCF4D', 'FFFFAC', 'F0BBBB', 'A43BA4',
		'8F8F8F', '7F6AFF', '72B6FF', '58B6FF', '22BBD4', '26CF26', '475300', 'F0841C', 'CC2E00', 'CF16CF',
		'6A6A6A', '1616D4', '0040DF', '0040FF', '47C0CF', '00FF16', '818B00', 'C7841C', 'CF0016', '940D9F',
		'161616', '0000FF', '0000FF', '0022FF', '00FFFF', '008300', '266326', 'AE0000', '830000', '8F0060',
		'000000', '0000A3', '00008C', '0016FF', '0097FF', '002200', '002200', 'FF0000', 'B10022', '810032',
		'000000', '000053', '000022', '00003B', '001616', '001600', '222200', '830000', '220000', '220022',
		'000000', '000022', '000016', '000040', '000000', '000000', '0D0000', '160000', '160000', '160016'
		]
	
	@Parameter(names = "-F", description = "Team file", required = true)
	String fname
	
	String teamName
	
	Team newTeam
	Team oldTeam
	
	Random random = new Random()
	
	@Override
	public void run() {
		// update group members
		updateReadOnlyGroup()
		
		newTeam = readTeamDSL(fname)
		teamName = newTeam.name
		
		readOldTeam()
		
		updateTeam()
	}

	//----------------------------------------------
	// Load team from DSL file
	//----------------------------------------------
	Team readTeamDSL(String fname) {
		log.info("Loading team information from ${fname}")
		
		Binding binding = new Binding()
		binding.team = Team.&loadTeam
		
		GroovyShell shell = new GroovyShell(binding)
		def result = shell.evaluate(new File(fname))
		
		return result
	}
	
	//----------------------------------------------
	// Export existing team from udeploy server
	//----------------------------------------------
	Team readOldTeam() {
		log.info("Read team $teamName from UrbanCode Deploy server ...")
		TeamExport exportCmd = new TeamExport(user: user, password: password, teamName: teamName)
		exportCmd.copyParams(this)
		exportCmd.query()
		
		oldTeam = exportCmd.team
	}
	
	//----------------------------------------------
	// update team resources
	//----------------------------------------------
	def updateTeam() {
		log.info("Update components...")
		newTeam.components.each {
			updateTeamComponent(it)
		}
		
		log.info("Update resource...")
		newTeam.resources.each {
			Resource oldRes = findTeamResource(oldTeam, it.name)
			updateTeamResource(oldRes, it, true)
		}
		
		log.info("Update applications...")
		newTeam.applications.each {
			updateTeamApplication(it)
		}
	}
	
	def findTeamResource(Team team, String name) {
		Resource res = null
		
		team.resources.find {
			if (it.name == name) {
				res = it
				return true
			}
		}
		
		return res
	}
	
	//----------------------------------------------
	// update component
	//----------------------------------------------
	def createComponent(Component comp) {
		log.info("Create component ${comp.name} ...")

		// may be a BUG: we cannot set properties during creating component...
		//
		def content = [ name: comp.name,
			description: comp.description,
			importAutomatically: false,
			defaultVersionType: comp.defaultVersionType
			
			/*
			properties: [
				repositoryName: comp.repositoryName,
				groupId: comp.groupId,
				artifactId: comp.artifactId
			]*/
		]
	
		if (comp.template?.trim()) {
			content['templateName'] = comp.template
			
			// DataStage uses incremental version type
			if (comp.template == 'DataStageTemplate') {
				content['defaultVersionType'] = 'INCREMENTAL'
			}
		}

		def result = httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + '/component/create'
			body = content
			
			// headers.'Accept' = ContentType.TEXT.getAcceptHeader()
			
			response.success = { resp, reader ->
				def txt = reader.text
				log.info("Succeed: $txt")
			}
		}
		
		setCompProp(comp.name, 'repositoryName', comp.repositoryName)
		setCompProp(comp.name, 'groupId', comp.groupId)
		setCompProp(comp.name, 'artifactId', comp.artifactId)
	}
	
	def setCompProp(String compName, String name, String value) {
		log.info("Set component property: $compName, $name=$value")
		def result = httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + '/component/propValue'
			uri.query = [component: compName, name: name, value: value]
		}
	}
	
	Component queryComponent(compName) {
		log.info("Query component $compName")
		
		def info = httpBuilder.get(path: PUBLIC_API + '/component/info',
			query: [component: compName])
		
		Component comp = new Component(id: info.id, name: info.name, description: info.description)
		comp.tags = info.tags
		comp.addTeams(info.extendedSecurity.teams)
		
		return comp
	}
	
	def tagComponent(Component comp) {
		// find if team is already tagged
		def tagged = false
		comp.tags.find { tag ->
			if (teamName == tag.name) {
				tagged = true
				return true
			}
		}

		if (tagged) {
			log.debug("Component ${comp.name} is already tagged with team $teamName")
			return
		}
			
		log.info("Add team tag to component ${comp.name}")
		
		def tagColor = TAG_COLORS[random.nextInt(TAG_COLORS.size())]
			
		httpBuilder.request(Method.PUT) { req ->
			uri.path = webUrl + PUBLIC_API + "/component/tag"
			uri.query = [ component: comp.name, tag: teamName, color: tagColor ]
			
			response.'204' = { resp, reader ->
			}
		}
	}
	
	def updateTeamComponent(Component newComp) {
		def oldComp = oldTeam.getComponent(newComp.name)
		
		def teamDefined = false
		
		if (oldComp == null) {
			createComponent(newComp)
			
			// query the new created component to remove unnecessary teams
			oldComp = queryComponent(newComp.name)
		}
		
		// fix teams
		oldComp.tsecs.each { team ->
			if (team.teamLabel == teamName && team.resourceRoleLabel == null) {
				teamDefined = true
			} else {
				log.info("Delete team ${team.teamLabel} (${team.resourceRoleLabel}) from component ${newComp.name}")
				
				httpBuilder.request(Method.DELETE) { req ->
					uri.path = webUrl + PUBLIC_API + "/component/teams"
					
					if (team.resourceRoleLabel == null) {
						uri.query = [ component: newComp.name, team: team.teamLabel ]
					} else {
						uri.query = [ component: newComp.name, team: team.teamLabel, type: team.resourceRoleLabel ]
					}
				}
			}
		}
				
		if (!teamDefined) {
			log.info("Add team ${teamName} to component ${newComp.name}")
				
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/component/teams"
				uri.query = [ component: newComp.name, team: teamName ]
			}
		}
		
		tagComponent(oldComp)
	}

	//----------------------------------------------
	// update application
	//----------------------------------------------
	def createApplication(Application app) {
		log.info("Create application ${app.name}")

		httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
			uri.path = PUBLIC_API + '/application/create'
			body = [name: app.name, notificationScheme: 'CPPIB', description: app.description]
		}
	}
	
	Application queryApplication(appName) {
		log.info("Query application $appName")
		
		def info = httpBuilder.get(path: PUBLIC_API + '/application/info',
			query: [application: appName])
		
		Application app = new Application(id: info.id, name: info.name, description: info.description)
		app.tags = info.tags
		app.addTeams(info.extendedSecurity.teams)
		
		return app
	}
	
	def tagApplication(Application app) {
		// find if team is already tagged
		def tagged = false
		app.tags.find { tag ->
			if (teamName == tag.name) {
				tagged = true
				return true
			}
		}

		if (tagged) {
			log.debug("Application ${app.name} is already tagged with team $teamName")
			return
		}
		
		log.info("Add team tag to application ${app.name}")
		
		def tagColor = TAG_COLORS[random.nextInt(TAG_COLORS.size())]
		
		httpBuilder.request(Method.PUT) { req ->
			uri.path = webUrl + PUBLIC_API + "/application/tag"
			uri.query = [ application: app.name, tag: teamName, color: tagColor ]
			
			response.'204' = { resp, reader ->
			}
		}
	}
	
	def updateTeamApplication(Application newApp) {
		def oldApp = oldTeam.getApplication(newApp.name)
		
		def teamDefined = false
		
		if (oldApp == null) {
			createApplication(newApp)
			oldApp = queryApplication(newApp.name)
		}
		
		// fix teams
		oldApp.tsecs.each { team ->
			if (team.teamLabel == teamName && team.resourceRoleLabel == null) {
				teamDefined = true
			} else {
				log.info("Delete team ${team.teamLabel} (${team.resourceRoleLabel}) from Application ${oldApp.name}")
				
				httpBuilder.request(Method.DELETE) { req ->
					uri.path = webUrl + PUBLIC_API + "/application/teams"
					
					if (team.resourceRoleLabel == null) {
						uri.query = [ application: oldApp.name, team: team.teamLabel ]
					} else {
						uri.query = [ application: oldApp.name, team: team.teamLabel, type: team.resourceRoleLabel ]
					}
				}
			}
		}
				
		if (!teamDefined) {
			log.info("Add team ${teamName} to Application ${oldApp.name}")
				
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/application/teams"
				uri.query = [ application: oldApp.name, team: teamName ]
			}
		}
		
		tagApplication(oldApp)
		
		addAppComponents(oldApp, newApp)
		
		updateApplicationEnvironments(oldApp, newApp)
	}
	
	//----------------------------------------------
	// update application component
	//----------------------------------------------
	def addAppComponents(Application oldApp, Application newApp) {
		newApp.comps.each { newComp ->
			Component oldComp = oldApp.getComponent(newComp.name)
			if (oldComp == null) {
				addAppComp(newApp.name, newComp.name)
			}
		}
	}
	
	def addAppComp(String appName, String compName) {
		log.info("Add component ${compName} to Application ${appName}")
		httpBuilder.request(Method.PUT) { req ->
			uri.path = webUrl + PUBLIC_API + "/application/addComponentToApp"
			uri.query = [ application: appName, component: compName ]
		}
	}
	
	//----------------------------------------------
	// update application environment
	//----------------------------------------------
	def updateApplicationEnvironments(Application oldApp, Application newApp) {
		newApp.envs.each { newEnv ->
			Environment oldEnv = oldApp.getEnvironment(newEnv.name)
			updateAppEnv(newApp.name, oldEnv, newEnv)
		}
	}

	def addAppEnv(String appName, Environment env) {
		log.info("Create environment: application $appName, environment ${env.name}")
		
		httpBuilder.request(Method.PUT, ContentType.TEXT) { req ->
			uri.path = webUrl + PUBLIC_API + "/environment/createEnvironment"
			uri.query = [ application: appName,
				name: env.name,
				description: (env.description == null) ? env.name : env.description ]
		}
	}
	
	Environment queryEnvironment(String appName, String envName) {
		def info = httpBuilder.get(path: PUBLIC_API + '/environment/info',
			query: [application: appName, environment: envName])
		
		def env = new Environment(id: info.id, name: info.name, description: info.description)
		env.addTeams(info.extendedSecurity.teams)
		
		return env
	}
	
	def updateAppEnv(String appName, Environment oldEnv, Environment newEnv) {
		if (oldEnv == null) {
			addAppEnv(appName, newEnv)
			oldEnv = queryEnvironment(appName, newEnv.name)
		}
		
		// update teams
		def teamDefined = false
		def ucdType = newEnv.getUcdType()
		
		// fix teams
		oldEnv.tsecs.each { team ->
			if (team.teamLabel == teamName && team.resourceRoleLabel == ucdType) {
				teamDefined = true
			} else {
				log.info("Delete team ${team.teamLabel} (${team.resourceRoleLabel}) from $appName ${newEnv.name}")
				
				def query = [ application: appName,
					environment: newEnv.name,
					team: team.teamLabel ]
				
				if (team.resourceRoleLabel != null) {
					query['type'] = team.resourceRoleLabel
				}
				
				httpBuilder.request(Method.DELETE) { req ->
					uri.path = webUrl + PUBLIC_API + "/environment/teams"
					uri.query = query
				}
			}
		}

		if (!teamDefined) {
			log.info("Add environment to team: application $appName, environment ${newEnv.name}, team $teamName, type $ucdType")
			
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/environment/teams"
				uri.query = [ application: appName,
					environment: newEnv.name,
					team: teamName,
					type: ucdType ]
			}
		}
		
		// setup artifactoryUrl in agent now
		// updateEnvProps(appName, oldEnv, newEnv)
				
		updateEnvResources(appName, oldEnv, newEnv)
	}
	
	//----------------------------------------------
	// update environment base resources
	//----------------------------------------------
	def updateEnvResources(String appName, Environment oldEnv, Environment newEnv) {
		newEnv.resources.each { newRes ->
			Resource oldRes = oldEnv.getResource(newRes.path)
			if (oldRes == null) {
				addEnvRes(appName, newEnv, newRes)
			}
		}
	}
	
	def addEnvRes(String appName, Environment env, Resource res) {
		log.info("Add resource to environment: application $appName, environment ${env.name}, resource ${res.path}")
		
		httpBuilder.request(Method.PUT) { req ->
			uri.path = webUrl + PUBLIC_API + "/environment/addBaseResource"
			uri.query = [ application: appName,
				environment: env.name,
				resource: res.path ]
		}
	}
	
	def updateEnvProps(String appName, Environment oldEnv, Environment newEnv) {
		def url = newEnv.getDefaultArtifactoryUrl()
		
		if (oldEnv.artifactoryUrl != url) {
			log.info("Define artifactoryUrl for: application=$appName, environment=${newEnv.name}")
			
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/environment/propValue"
				uri.query = [ application: appName,
					environment: newEnv.name,
					name: 'artifactoryUrl',
					value: url,
					isSecure: false ]
			}
		}
	}
	
	//----------------------------------------------
	// update resources
	//----------------------------------------------
	def _fixResourceTeam(Resource oldRes, Resource newRes) {
		def teamDefined = false
		def ucdType = newRes.getUcdType()
		def path = newRes.getPath()
		
		if (oldRes.inheritTeam) {
			log.info("Resource $path inherits team, query resources again")
			oldRes = queryResource(newRes.getPath())
		}

		oldRes.tsecs.each { team ->
			if (team.teamLabel == teamName && team.resourceRoleLabel == ucdType) {
				teamDefined = true
			} else {
				log.info("Delete team ${team.teamLabel} (${team.resourceRoleLabel}) from resource $path")
				
				httpBuilder.request(Method.DELETE) { req ->
					uri.path = webUrl + PUBLIC_API + "/resource/teams"
					
					if (team.resourceRoleLabel == null) {
						uri.query = [ resource: path, team: team.teamLabel ]
					} else {
						uri.query = [ resource: path, team: team.teamLabel, type: team.resourceRoleLabel ]
					}
				}
			}
		}
		
		if (!teamDefined) {
			log.info("Add team ${teamName} ($ucdType) to resource $path")
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/resource/teams"
				
				if (ucdType == null) {
					uri.query = [ resource: path, team: teamName ]
				} else {
					uri.query = [ resource: path, team: teamName, type: ucdType ]
				}
			}
		}
	}
	
	// return oldRes
	def updateTeamResource(Resource oldRes, Resource newRes, boolean isTop) {
		if (oldRes == null) {
			createResource(newRes)
			oldRes = queryResource(newRes.getPath())
		}
		
		_fixResourceTeam(oldRes, newRes)
		oldRes.inheritTeam = false
		
		// setResourceSecure(oldRes, newRes)
		
		if (isTop) {
			tagResource(oldRes)
			updateResourceITAM(oldRes, newRes)
		}
		
		if (newRes.getDisplayCategory() == Resource.CATEGORY_AGENT) {
			String appName = newRes.getApplication()
			updateAgent(newRes.name, appName)
		}
		
		// update children
		newRes.children.each { newChild ->
			Resource oldChild = null
			if (oldRes != null) {
				oldChild = oldRes.getResourceByName(newChild.name)
			}
			
			updateTeamResource(oldChild, newChild, false)
		}
	}
	
	def updateResourceITAM(Resource oldRes, Resource newRes) {
		if (oldRes.ITAMAppID != newRes.ITAMAppID) {
			log.info("Resource update ITAMAppID: ${oldRes.name}, ITAMAppID=${newRes.ITAMAppID}")
			
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/resource/setProperty"
				uri.query = [ resource: oldRes.id, name: 'ITAMAppID', value: newRes.ITAMAppID ]
			}
		}
		
		if (oldRes.ITAMTeamName != newRes.ITAMTeamName) {
			log.info("Resource update ITAMTeamName: ${oldRes.name}, ITAMTeamName=${newRes.ITAMTeamName}")
			
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/resource/setProperty"
				uri.query = [ resource: oldRes.id, name: 'ITAMTeamName', value: newRes.ITAMTeamName ]
			}
		}
	}
	
	void createResource(Resource res) {
		String path = res.getPath()
		log.info("Create resource $path")
		
		def category = res.getDisplayCategory()
		
		def roleId = null
		if (category == Resource.CATEGORY_COMP) {
			roleId = getComponentRoleId(res.name)
		}
		
		def content = [name: res.name,
			description: res.description
			]
		
		if (res.parent != null) {
			content['parent'] = res.parent.getPath()
		}
		
		if (category == Resource.CATEGORY_AGENT) {
			content['agent'] = res.name
		} else if (category == Resource.CATEGORY_COMP) {
			content['role'] = roleId
		}
		
		if (res.getImpersonationUser() != null) {
			content['impersonationUser'] = res.getImpersonationUser()
			content['impersonationForce'] = false
			content['impersonationUseSudo'] = true
		}
		
		httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + "/resource/create"
			body = content
	
			response.success = { resp, reader ->
				def txt = reader.text
				log.info("Succeed: $txt")
			}
		}
	}
	
	String getComponentRoleId(String name) {
		// get component Role id
		def li = httpBuilder.get(path: '/rest/resource/resourceRole/componentRoles/',
			query: [name: name])
		
		def roleId = li[0].id
		return roleId
	}
	
	void setResourceSecure(Resource oldRes, Resource newRes) {
		def change = false
		
		if (!newRes.isSecure()) {
			return
		}
		
		if (oldRes != null) {
			if (!oldRes.isSecure()) {
				change = true
			}
		}
		
		if (change) {
			def path = newRes.getPath()
			log.info("Set secure to resource $path")
			
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/resource/tag"
				uri.query = [resource: path,
					tag: 'Secure',
					description: 'The resource contains secure properties']
			}
		}
	}
	
	def tagResource(Resource res) {
		// find if team is already tagged
		def tagged = false
		res.tags.find { tag ->
			if (teamName == tag.name) {
				tagged = true
				return true
			}
		}

		if (tagged) {
			log.debug("Resource ${res.name} is already tagged with team $teamName")
			return
		}
			
		log.info("Add team tag to resource ${res.name}")
		
		def tagColor = TAG_COLORS[random.nextInt(TAG_COLORS.size())]
			
		httpBuilder.request(Method.PUT) { req ->
			uri.path = webUrl + PUBLIC_API + "/resource/tag"
			uri.query = [ resource: res.id, tag: teamName, color: tagColor ]
			
			response.'204' = { resp, reader ->
			}
		}
	}
	
	Resource queryResource(path) {
		log.info("Query resource $path")
		
		def info = httpBuilder.get(path: PUBLIC_API + '/resource/info',
			query: [resource: path])
		
		Resource res = new Resource(id: info.id, name: info.name, description: info.description, path: info.path)
		res.tags = info.tags
		res.inheritTeam = info.inheritTeam
		res.addTeams(info.extendedSecurity.teams)
		
		return res
	}
	
	def updateAgent(String agentName, String appName) {
		log.debug("Check if agent $agentName is already assigned to team $teamName")
		def info = httpBuilder.get(path: PUBLIC_API + '/agentCLI/info',
			query: [agent: agentName])

		def teamAssigned = false
		info.extendedSecurity.teams.find { team ->
			if (teamName == team.teamLabel) {
				teamAssigned = true
				return true
			}
		}
		
		if (!teamAssigned) {
			log.info("Add team $teamName to agent $agentName")
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/agentCLI/teams"
				uri.query = [ agent: agentName, team: teamName ]
				
				response.'204' = { resp, reader ->
				}
			}
		}
		
		// set artifactoryUrl
		def url = Environment.getAgentArtifactoryUrl(agentName)
		
		// get artifactoryUrl
		def oldUrl = null
		
		httpBuilder.request(Method.GET, ContentType.TEXT) { req ->
			uri.path = PUBLIC_API + '/agentCLI/getProperty'
			uri.query = [agent: agentName, name: 'artifactoryUrl']
			
			response.success = { resp, reader ->
				oldUrl = reader.text
			}
				 
			response.'404' = { resp, reader ->
				log.debug("Agent $agentName property artifactoryUrl is not defined")
			}
		}
		
		if (url != oldUrl) {
			if (oldUrl == null) {
				log.info("Define artifactoryUrl for: agent=$agentName, url=$url")
			} else {
				log.warn("Agent artifactoryUrl is different! agent.artifactoryUrl=$oldUrl, suggest.artifactoryUrl=$url")
				return
			}
			
			httpBuilder.request(Method.PUT) { req ->
				uri.path = webUrl + PUBLIC_API + "/agentCLI/setProperty"
				uri.query = [ agent: agentName,
					name: 'artifactoryUrl',
					value: url,
					isSecure: false ]
			}
		}

		// tag agent
		// find if appName is already tagged
		if (appName) {
			def tagged = false
			info.tags.find { tag ->
				if (appName == tag.name) {
					tagged = true
					return true
				}
			}
	
			if (tagged) {
				log.debug("Agent $agentName is already tagged appName $appName")
			} else {
				log.info("Agent: add tag $appName to agent $agentName")
				def tagColor = TAG_COLORS[random.nextInt(TAG_COLORS.size())]
					
				httpBuilder.request(Method.PUT) { req ->
					uri.path = webUrl + PUBLIC_API + "/agentCLI/tag"
					uri.query = [ agent: agentName, tag: appName, color: tagColor ]
					
					response.'204' = { resp, reader ->
					}
				}
			}
		}
	}
	
	//----------------------------------------------
	// update ReadOnly group members
	//----------------------------------------------
	void updateReadOnlyGroup() {
		GroupManager groupMgr = new GroupManager()
		groupMgr.copyParams(this)
		groupMgr.updateReadOnlyGroup()
	}
}
