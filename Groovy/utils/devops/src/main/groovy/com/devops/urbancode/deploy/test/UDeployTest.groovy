package com.devops.urbancode.deploy.test

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.Method

import org.codehaus.groovy.runtime.StackTraceUtils

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters
import com.devops.urbancode.admin.EnvPropsUpdate
import com.devops.urbancode.admin.TeamImport
import com.devops.urbancode.admin.UdeployCmd
import com.devops.urbancode.deploy.model.Application
import com.devops.urbancode.deploy.model.Component
import com.devops.urbancode.deploy.model.Environment
import com.devops.urbancode.deploy.model.Resource
import com.devops.urbancode.deploy.model.Team
 
/* Test UDeploy with REST API
 * 
 * This class is used after UDeploy upgrade
 * 
 *
 */
@Slf4j
@Parameters(commandDescription = "Test uDeploy with REST API")
class UDeployTest extends UdeployCmd {
	static String OP_TEST = 'TEST'
	static String OP_DONE = 'DONE'
	
	@Parameter(names = "-CF", description = "Team config file", required = false)
	String confFile
	
	@Parameter(names = "-PF", description = "Properties file", required = false)
	String propFile
	
	@Parameter(names = "-OP", description = "Operation: TEST | DONE", required = true)
	String operation

	Team team
	TeamImport teamImport
	//EnvPropsUpdate propsUpdate
	
	@Override
	public void run() {
		if (operation == 'TEST') {
			runTest()
		} else if (operation == 'DONE') {
			runDone()
		}
	}

	void runTest() {
		if (confFile != null) {
			log.info('Test team config setup...')
			teamImport = new TeamImport()
			teamImport.copyParams(this)
			teamImport.fname = confFile
			
			teamImport.run()
		}
		
		if (propFile != null) {
			log.info('Test environment properties ...')
			propsUpdate = new EnvPropsUpdate()
			propsUpdate.copyParams(this)
			propsUpdate.fileName = propFile
			propsUpdate.includeResources = true
			
			propsUpdate.run()
		}
	}
	
	void runDone() {
		if (confFile != null) {
			clearTeamConfig()
		}
		
		if (propFile != null) {
		}
	}
	
	void clearTeamConfig() {
		log.info('Clear team config ...')
		teamImport = new TeamImport()
		teamImport.copyParams(this)
		teamImport.fname = confFile
		team = teamImport.readTeamDSL(confFile)
	
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
	
	void clearComponent(Component comp) {
		log.info("Remove component ${comp.name} from team ${team.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + '/component/teams'
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
			uri.path = webUrl + PUBLIC_API + '/application/teams'
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
				uri.path = webUrl + PUBLIC_API + '/environment/removeBaseResource'
				uri.query = [environment: env.name, application: app.name, resource: res.path]
			}
		}
				
		log.info("Remove environment ${env.name} from team ${team.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + '/environment/teams'
			uri.query = [environment: env.name, application: app.name, team: team.name, type: env.getUcdType()]
		}

		log.info("Remove environment ${env.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + '/environment/deleteEnvironment'
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
			uri.path = webUrl + PUBLIC_API + '/resource/teams'
			uri.query = [resource: res0.id, team: team.name, type: res.getUcdType()]
		}
		
		log.info("Remove resource ${res.name}")
		httpBuilder.request(Method.DELETE, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + '/resource/deleteResource'
			uri.query = [resource: res0.id]
		}
	}
	
	public static void main(String[] args) {
		UDeployTest test = new UDeployTest()

		JCommander jcmd = new JCommander(test)
		
		try {
			jcmd.parse(args)
			
			test.init()
			test.process()
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(-1)
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("UDeploy test FAILED", ta)
			System.exit(-2)
		}
		
		System.exit(0)
	}
}
