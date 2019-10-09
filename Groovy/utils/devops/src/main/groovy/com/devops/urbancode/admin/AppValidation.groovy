package com.devops.urbancode.admin

import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters
import com.devops.urbancode.deploy.model.Application
import com.devops.urbancode.deploy.model.Component
import com.devops.urbancode.deploy.model.Environment
import com.devops.urbancode.deploy.model.Resource

@Slf4j
@Parameters(commandDescription = "Validate resource used by an application under an environment")
class AppValidation extends UdeployCmd {	
	
	List<String> envcomps=[] as String[]
	
	@Parameter(names = "-APP", description = "Application name", required = true)
	String appName

	@Parameter(names = "-ENV", description = "Enironment name", required = true)
	String envName
		
	@Override
	public void run() {		
		queryApplication(appName)
	}
	
	def queryApplication(String appName) {
		def info = httpBuilder.get(path: PUBLIC_API + '/application/info',
			query: [application: appName])
		
		Application app = new Application(name: info.name, id: info.id, description: info.description)
					
		def envList = httpBuilder.get(path: PUBLIC_API + '/application/environmentsInApplication',
			query: [application: app.name])

		def IsValidEnv = false
		envList.each {
			if (it.name == envName) {
				IsValidEnv = true
				Environment env = queryEnvironement(app, it.name)
				app.envs.add(env)
			}		
		}
						
		if (!IsValidEnv) {				
			throw new Exception('Invalid Environment Name: ' + envName)	
		}
				
		queryApplicationComponents(app)
		
		app.comps.each {			
			if (!envcomps.contains(it.name)){				
				throw new Exception('Missing environment: ' + envName + ' for component ' + it.name)
			}									
		}			
				
	}
		
	def queryApplicationComponents(Application app) {
		def li = httpBuilder.get(path: PUBLIC_API + '/application/componentsInApplication',
			query: [application: app.name])
		
		li.each {
			Component comp = new Component(id: it.id, name: it.name)
			app.comps.add(comp)
		}
	}
	
	Environment queryEnvironement(Application app, String envName) {
		def info = httpBuilder.get(path: PUBLIC_API + '/environment/info',
			query: [environment: envName, application: app.name])
		
		Environment env = new Environment(name: info.name, id: info.id, description: info.description)
		env.addTeams(info.extendedSecurity.teams)
		
		// base resources
		def baseResourceList = httpBuilder.get(path: PUBLIC_API + '/environment/getBaseResources',
			query: [application: app.name, environment: envName])	
		
		baseResourceList.each {
			//Resource res = new Resource(name: it.name, id: it.id, path: it.path)
			Resource res = queryResource(it.id)		
			env.resources.add(res)
		}
		
		return env
	}
	
	Resource queryResource(String id) {
		def info = httpBuilder.get(path: PUBLIC_API + '/resource/info',
			query: [resource: id])
		
		Resource res = new Resource(name: info.name, id: info.id, description: info.description)
		
		// query child resources
		def childList = httpBuilder.get(path: PUBLIC_API + '/resource',
			query: [parent: res.id])		
		
		childList.each {
			Resource child = queryResource(it.id)			
			envcomps.add(it.name)
			
			if (child != null) {
				res.addChild(child)
			}
		}

		return res
	}

	public static void main(String[] args) {
		AppValidation appValidation = new AppValidation()
		JCommander jcmd = new JCommander(appValidation)
		
		try {
			jcmd.parse(args)
			appValidation.init()
			appValidation.process()			
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(0)
			
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("AppValidation FAILED", ta)
			System.exit(-1)
		}
		
		log.info("AppValidation SUCCESS")
		System.exit(0)
	}
}
