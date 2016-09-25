package com.devops.urbancode.admin

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import org.codehaus.groovy.runtime.StackTraceUtils

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters
import com.devops.urbancode.deploy.DeployClient
import com.devops.urbancode.deploy.DeployRestAPI
import com.devops.utils.ConfigFile

/**
 * Update udeploy:
 * - create component version if required
 * - add/update artifactory information on the component version
 * 
 * @author 
 *
 */
@Slf4j
@Parameters(commandDescription = "Update udeploy component version")
class UdeployUpdate {
	static String KEY_FILE = '.devops'

	static String REPO = "artifactoryRepo"
	static String GROUP_ID = "groupId"
	static String ARTIFACT_ID = "artifactId"
	static String COMMENTS = "comments"
	
	static final String UCD_USER = 'urbancode.deploy.user'
	static final String UCD_PASSWORD = 'urbancode.deploy.password'
	
	static final String UCDCLI = 'UCDCLI'
	
	@Parameter(names = "-COMP", description = "Component Name", required = true)
	String component
	
	@Parameter(names = "-VER", description = "Component Version (same as artifact version)", required = true)
	String version
	
	@Parameter(names = "-U", description = "User name, if not specified use urbancode.deploy.user", required = false)
	String user
	
	// In urbancode we use component properties to associate with repository, groupdId, and artifactId
	// So we only need version in the command
	//
	/*
	@Parameter(names = "-REPO", description = "Artifactory Repository", required = true)
	String repo
	
	@Parameter(names = "-GID", description = "groupId", required = true)
	String groupId
	
	@Parameter(names = "-AID", description = "artifactId", required = true)
	String artifactId

	@Parameter(names = "-COMMENT", description = "comments", required = false)
	String comments
	*/
	
	private ConfigFile conf
	
	HTTPBuilder httpBuilder
	DeployRestAPI deployAPI
	
	static ConfigFile loadConf() {
		// config and keyFile are located at $user.home/devops/conf

		// keyFile
		File confDir = new File(System.getProperty('user.home') +  '/devops/conf')
		File keyFile = new File(confDir, KEY_FILE)
			
		// load confFile
		File confFile = new File(confDir, 'devops.properties')
		if (!confFile.exists()) {
			throw new Exception("Conf file $confFile is not exist")
		}
		
		log.info("Load conf file from $confFile")
		return new ConfigFile(confFile, keyFile)
	}
	
	static def getCredential(ConfigFile conf, String userName) {
		def user
		def password
		
		if (userName?.trim()) {
			//user = userName
			user = conf.getConfig(userName)
			password = conf.getConfig(user + '.password', true)			
		} else {
			user = conf.getConfig(DeployClient.UCD_USER)
			password = conf.getConfig(DeployClient.UCD_PASSWORD, true)
		}
		
		return [user: user, password: password]
	}
	
	UdeployUpdate() {
	}
	
	// Use CLI to create version
	void process() {
		boolean usingCLI = Boolean.parseBoolean(System.getProperty(UCDCLI, 'false'))
		if (!usingCLI) {
			process2()
			return
		}
		
		if (conf == null) {
			conf = loadConf()
		}
		
		log.info("Update component version: component=$component, version=$version")
		
		DeployClient deployClient = new DeployClient(conf)
		
		try {
			def cred = getCredential(conf, user)
			deployClient.login(cred.user, cred.password)
			deployClient.createVersion(component, version)
			
			/*
			deployClient.createComponentVersionPropDefs(component, [REPO, GROUP_ID, ARTIFACT_ID, COMMENTS])
			
			def props = [(REPO):repo, (GROUP_ID): groupId, (ARTIFACT_ID): artifactId]
			if (comments?.trim()) {
				props[COMMENTS] = comments
			}
				
			deployClient.setComponentVersionProperties(component, version, props)
			*/
		} finally {
			deployClient.logout()
		}
	}
	
	// Use REST API to create version
	void process2() {
		if (conf == null) {
			conf = loadConf()
		}
		
		deployAPI = new DeployRestAPI(conf)
		
		try {
			def cred = getCredential(conf, user)
		
			log.info("Login ${deployAPI.webUrl} ... with user ${cred.user}")
			httpBuilder = deployAPI.login(cred.user, cred.password)
			
			// set default failure handler
			httpBuilder.handler.failure = { resp ->
				def req = resp.context['http.request']
				
				log.error("HTTP request failed: ${req.method} ${req.URI}")
				log.error("     response status ${resp.status} ${resp.statusLine}")
				
				throw new Exception("HTTP failure")
			}
			
			createVersion()
		} finally {
			if (httpBuilder != null) {
				deployAPI.logout(httpBuilder)
			}
		}
	}
	
	def createVersion() {
		log.info("Create component version: component=$component, version=$version")
		
		httpBuilder.request(Method.POST) { req ->
			uri.path = '/cli/version/createVersion'
			uri.query = [component: component, name: version]
			headers.'Accept' = ContentType.JSON.getAcceptHeader()
			
			response.success = { resp, reader ->
				log.info("Succeeded: $component version $version is created")
			}
			
			response.failure = { resp, reader ->
				def text = reader.text
				
				def found = (text =~ /(.*)Version with name $version already exists for Component#(.*) \((.*)\)/)			
				if (found.count > 0) {
					log.info("Component $component with version $version already exists")
				} else {
					log.error("Failed: status ${resp.status} ${resp.statusLine}")
					log.error("        $text")
					throw new Exception("Create version failed")
				}
			}
		}
	}
	
	public static void main(String[] args) {
		UdeployUpdate udupdate = new UdeployUpdate()
		JCommander jcmd = new JCommander(udupdate)
		
		try {
			jcmd.parse(args)
			udupdate.process()
			System.exit(0)
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(0)
			
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("UdeployUpload FAILED", ta)
			System.exit(-1)
		}
		
		System.exit(0)
	}
}

