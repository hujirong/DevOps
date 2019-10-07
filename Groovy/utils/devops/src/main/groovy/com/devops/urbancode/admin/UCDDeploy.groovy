package com.devops.urbancode.admin

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.Method

import org.codehaus.groovy.runtime.StackTraceUtils

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters

/**
 * Deploy application in UCD
 * @author fhou
 *
 * This application takes a Deploy config file and trigger deployment on UCD
 * It starts deployment, and checks status periodically until it is:
 * 		- SUCCEED
 * 		- FAILURE
 * 		- TIMEOUT
 * 
 * The config file examples are in wiki:
 *     https://wiki.cppib.ca:8443/display/CORE/uDeploy+-+Use+ucd-deploy+To+Deploy+Application
 */
@Slf4j
@Parameters(commandDescription = "Deploy application with UrbanCode")
class UCDDeploy extends UdeployCmd2 {
	// status check period (in seconds)
	static long SLEEP = 5
	static String SUCCEED = 'SUCCEEDED'
	
	@Parameter(names = "-F", required = true, description = "Deploy configuration file, examples in https://wiki.cppib.ca:8443/display/CORE/uDeploy+-+Use+ucd-deploy+To+Deploy+Application")
	String deployConfFile
	
	@Parameter(names = "-DEPLOY", required = false, arity = 0,
		description = "DEPLOY flag. If specify it, verify deploy config file, and then deploy the application.")
	boolean deployFlag = false
	
	@Parameter(names = "-ONLY_CHANGED", required = false, arity = 1,
		description = "Deploy only changed versions, default is false.")
	boolean onlyChanged = true
	
	def deployXml
	def appXml
	
	boolean deployComponents = false
	boolean deploySnapshot = false
	
	String result = "FALIED"
	
	// componets in resource tree
	def resComps = []
	
	def loadDeployConfFile() {
		log.info("Load deploy configuration file: $deployConfFile")
		File file = new File(deployConfFile)
		assert file.exists() : "File ${file.getCanonicalPath()} is not exist."
		
		def parser = new XmlParser()
		deployXml = parser.parse(file)
		appXml = deployXml.application[0]
		
		// verify config
		assert appXml.'@name' : "Missing application name in $file"
		assert appXml.'@environment' : "Missing application environment in $file"
		assert appXml.process[0].'@name' : "Missing application process in $file"
		
		if (!appXml.components.component.isEmpty()) {
			deployComponents = true
		}
		
		if (!appXml.snapshot.isEmpty()) {
			deploySnapshot = true
		}
		
		assert (!(deployComponents && deploySnapshot)) : "You can only specify either components or snapshot"
	}
	
	// verify deployConf with UCD server
	void verify() {
		// verify application environment
		
		log.info("====> Verify application ${appXml.'@name'} environment ${appXml.@'environment'}")
		def env = httpBuilder.get(path: PUBLIC_API + '/environment/info',
			query: [application: appXml.'@name', environment: appXml.'@environment'])
		
		def baseResources = httpBuilder.get(path: PUBLIC_API + '/environment/getBaseResources',
			query: [application: appXml.'@name', environment: appXml.'@environment'])
		
		assert (baseResources.size() > 0) : "Environment $appXml.'@environment' is not mapped to resource tree"

		baseResources.each {
			verifyResource(it)
		}
		
		def appComps = httpBuilder.get(path: PUBLIC_API + '/application/componentsInApplication', query: [application: appXml.'@name'])
			
		// verify component versions
		if (deployComponents) {
			appXml.components.component.each {
				verifyComponent(it.'@name', it.'@version', appComps, appXml)			}
		}
		
		if (deploySnapshot) {
			log.info("Verify application ${appXml.'@name'} snapshot ${appXml.snapshot.'@name'}")
			def compVers = httpBuilder.get(path: PUBLIC_API + '/snapshot/getSnapshotVersions',
				query: [application: appXml.'@name', snapshot: appXml.snapshot.'@name'])
			
			// verify component versions in snapshot
			compVers.each {
				def deployComp = [name: it.name, version: it.desiredVersions.name]
				verifyComponent(it.name, it.desiredVersions.name, appComps, appXml)
			}
		}
	}

	void verifyComponent(compName, compVersion, appComps, appXml) {
		log.info("Verify component $compName version $compVersion")
		boolean found = false
		appComps.each {
			if (it.name == compName.toString()) {
				found = true
				return
			}
		}
		
		assert found : "Cannot find component $compName in application ${appXml.'@name'}"
		
		httpBuilder.get(path: PUBLIC_API + '/version/versionProperties',
			query: [component: compName, version: compVersion])
		
		// verify component is mapped to resource tree
		assert resComps.contains(compName): "Cannot find component $compName from environment ${appXml.'@environment'} baseResources"
	}
	
	boolean verifyResource(res) {
		def info = httpBuilder.get(path: PUBLIC_API + '/resource/info', query: [resource: res.id])
		
		if (info.type == 'agent') {
			verifyAgent(res.name)
		} else if (info.type == 'subresource' && info.role && info.role.specialType == 'COMPONENT') {
			resComps.add(info.name)
			return
		}
		
		def children = httpBuilder.get(path: PUBLIC_API + '/resource', query: [parent: res.id])
		children.each {
			verifyResource(it)
		}
	}
	
	boolean verifyAgent(agentName) {
		log.info("Verify agent $agentName is ONLINE")
		def info = httpBuilder.get(path: PUBLIC_API + '/agentCLI/info', query: [agent: agentName])
		
		assert (info.status == "ONLINE") : "Agent $agentName status is ${info.status}"
		
		log.info("Verify agent $agentName property artifactoryUrl")		
		httpBuilder.get(path: PUBLIC_API + '/agentCLI/getProperty', query: [agent: agentName, name: 'artifactoryUrl'])
	}
	
	void deploy() {
		log.info("====> Deploy application ${appXml.'@name'} environment ${appXml.'@environment'}")
		
		def query = [application: appXml.'@name',
			applicationProcess: appXml.process[0].'@name',
			environment: appXml.'@environment',
			description: 'UCDDeploy does it!',
			onlyChanged: onlyChanged]
		
		// properties
		def props = [:]
		appXml.process[0].property.each {
			props.put(it.'@name', it.'@value')
		}
		
		if (props.size() > 0) {
			query.properties = props
		}
		
		if (deployComponents) {
			def versions = []
			appXml.components.component.each {
				versions.add([component: it.'@name', version: it.'@version'])
			}
			
			query.versions = versions
		}
		
		if (deploySnapshot) {
			query.snapshot = appXml.snapshot[0].'@name'
		}
		
		log.info("Application process request parameters: $query")
		def queryJson = JsonOutput.toJson(query)
		def info = httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
			uri.path = PUBLIC_API + '/applicationProcessRequest/request'
			body = queryJson
		}

		def requestId = info.requestId
		log.info("Check request status for $requestId")
		
		String status = "RUNNING"
		
		while (status != "CLOSED") {
			sleep(SLEEP * 1000)
			info = httpBuilder.get(path: PUBLIC_API + '/applicationProcessRequest/requestStatus', query: [request: requestId])
			status = info.status
			result = info.result
			log.info("    status: $status,     result: $result")
		}
		
		if (result == SUCCEED) {
			log.info("Deploy $result")
		} else {
			log.error("Deploy $result")
		}
	}
	
	@Override
	public void run() {
		verify()
		if (deployFlag) {
			deploy()
		}
	}

	public static void main(String[] args) {
		UCDDeploy deployer = new UCDDeploy()
		JCommander jcmd = new JCommander(deployer)
		
		try {
			jcmd.parse(args)
			
			deployer.init()
			deployer.loadDeployConfFile()
			deployer.process()
			
			if (deployer.result != SUCCEED) {
				System.exit(2)
			}
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(0)
			
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("UCDDeploy FAILED", ta)
			System.exit(-1)
		}
	}
}
