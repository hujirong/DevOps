

package com.cppib.core.urbancode.test

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import com.devops.admin.TeamImport
import com.devops.urbancode.deploy.DeployRestAPI
import com.devops.urbancode.deploy.model.Application
import com.devops.urbancode.deploy.model.Component
import com.devops.urbancode.deploy.model.Environment
import com.devops.urbancode.deploy.model.Resource
import com.devops.urbancode.deploy.model.Team

class TeamManagerTest {
	static Log log = LogFactory.getLog(TeamManagerTest.class)
	
	static String TEAM_FILE = 'src/test/resources/TeamConfigTest.conf'
	UTestConf utestConf

	Team team
	HTTPBuilder httpBuilder
	
	TeamImport teamImport
	
	@BeforeTest	
	void init() {
		utestConf = new UTestConf()
	}
	
	def createTeamImport() {
		teamImport = new TeamImport()
		teamImport.fname = TEAM_FILE
		
		teamImport.user = utestConf.user
		teamImport.conf = utestConf.conf
		teamImport.webUrl = utestConf.getBaseUrl()
		
		teamImport.deployAPI = new DeployRestAPI(utestConf.conf)
		teamImport.deployAPI.webUrl = teamImport.webUrl
	}
	
	void loginAndTest(Closure block) {
		try {
			String passwordKey = utestConf.user + '.password'
			teamImport.httpBuilder = teamImport.deployAPI.login(utestConf.user, utestConf.conf.getConfig(passwordKey, true))
			
			httpBuilder = teamImport.httpBuilder
			block()
		} finally {
			if (teamImport.httpBuilder != null) {
				teamImport.deployAPI.logout(httpBuilder)
				httpBuilder = null
			}
		}

	}

	@Test
	void testCreate() {
		log.info('Test team config setup...')
		createTeamImport()
			
		loginAndTest {
			teamImport.run()
		}
	}

	
	@Test(dependsOnMethods = ['testCreate'])
	void testDelete() {
		log.info('Clear team config ...')
		
		createTeamImport()
		
		loginAndTest {
			team = teamImport.readTeamDSL(teamImport.fname)
			
			team.applications.each {
				clearApplication(it)
			}
			
			team.resources.each {
				clearResource(it)
			}
			
			team.components.each {
				clearComponent(it)
			}
		}
	}
	
	void clearComponent(Component comp) {
		log.info("Remove component ${comp.name} from team ${team.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = teamImport.webUrl + teamImport.PUBLIC_API + '/component/teams'
			uri.query = [component: comp.name, team: team.name]
		}

		Component comp0 = teamImport.queryComponent(comp.name)
		
		log.info("Remove component ${comp.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = "/rest/deploy/component/${comp0.id}"
		}
	}
	
	void clearApplication(Application app) {
		Application app0 = teamImport.queryApplication(app.name)
		
		app.comps.each { Component comp ->
			log.info("Remove component ${comp.name} from application ${app.name}")
			
			Component comp0 = teamImport.queryComponent(comp.name)
			
			httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
				uri.path = "/rest/deploy/application/${app0.id}/removeComponents"
				body = [components: [comp0.id]]
			}
		}
		
		app.envs.each { Environment env ->
			clearEnvironment(app, env)
		}
		
		log.info("Remove application ${app.name} from team ${team.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = teamImport.webUrl + teamImport.PUBLIC_API + '/application/teams'
			uri.query = [application: app.name, team: team.name]
		}

		log.info("Remove application ${app.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = "/rest/deploy/application/${app0.id}"
		}
	}
	
	void clearEnvironment(Application app, Environment env) {
		env.resources.each { Resource res ->
			log.info("Remove resource ${res.path} from env ${env.name}")
			httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
				uri.path = teamImport.webUrl + teamImport.PUBLIC_API + '/environment/removeBaseResource'
				uri.query = [environment: env.name, application: app.name, resource: res.path]
			}
		}
				
		log.info("Remove environment ${env.name} from team ${team.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = teamImport.webUrl + teamImport.PUBLIC_API + '/environment/teams'
			uri.query = [environment: env.name, application: app.name, team: team.name, type: env.getUcdType()]
		}

		log.info("Remove environment ${env.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = teamImport.webUrl + teamImport.PUBLIC_API + '/environment/deleteEnvironment'
			uri.query = [environment: env.name, application: app.name]
		}
	}
	
	void clearResource(Resource res) {
		res.children.each {
			clearResource(it)
		}
		
		Resource res0 = teamImport.queryResource(res.path)
		
		log.info("Remove resource ${res.name} from team ${team.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = teamImport.webUrl + teamImport.PUBLIC_API + '/resource/teams'
			uri.query = [resource: res0.id, team: team.name, type: res.getUcdType()]
		}
		
		log.info("Remove resource ${res.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = teamImport.webUrl + teamImport.PUBLIC_API + '/resource/deleteResource'
			uri.query = [resource: res0.id]
		}
	}
}
