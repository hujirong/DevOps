package com.devops.urbancode.deploy

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import com.devops.Env;
import com.devops.utils.ConfigFile;

/**
 * Access UrbanCode deploy with REST API
 * @author 
 *
 */
@Slf4j
class DeployClient {
	static final String UCD_URL = 'urbancode.deploy.server'
	static final String UCD_USER = 'urbancode.deploy.user'
	static final String UCD_PASSWORD = 'urbancode.deploy.password'
	static final String UCD_PROXY_HOST = 'urbancode.deploy.proxyHost'
	static final String UCD_PROXY_PORT = 'urbancode.deploy.proxyPort'
	
	private File defaultKeyFile = new File(System.getProperty('user.home') + '/devops/conf/.devops')

	// config file
	ConfigFile conf
	
	private String webUrl
	private String proxyHost
	private String proxyPort
	private String ucdClient
	
	private JsonSlurper jsonSlurper = new JsonSlurper()
	
	private boolean loggedIn = false;
		
	DeployClient(File confFile, File keyFile = null) {
		if (keyFile == null) {
			keyFile = this.defaultKeyFile
		}

		conf = new ConfigFile(confFile, keyFile)
		init()
	}
	
	DeployClient(ConfigFile uconfFile) {
		conf = uconfFile
		init()
	}
	
	private void init() {
		webUrl = conf.getConfig(UCD_URL)
		proxyHost = conf.getConfig(UCD_PROXY_HOST)
		proxyPort = conf.getConfig(UCD_PROXY_PORT)
		
		def cmd = 'udclient'
		if (System.getProperty('os.name').startsWith('Windows')) {
			cmd = cmd + '.cmd'
		}

		ucdClient = Env.APP_HOME + "/udclient/$cmd"
		ucdClient = new File(ucdClient).getCanonicalPath()
	}
	
	public runSession(Closure closure) {
		runSession(null, null, closure)
	}
	
	public runSession(String user, String password, Closure closure) {
		try {
			login(user, password)
			closure.call()	
		} finally {
			logout()
		}
	}
	
	private String runCommand(List cmd) {
		def (exitCode, out) = runCommand2(cmd)
		if (exitCode != 0) {
			throw new Exception("exitCode: $exitCode, error: $out")
		}
		
		return out
	}
	
	private def runCommand2(List cmd) {
		def sb = new StringBuilder()
		def proc = new ProcessBuilder(cmd).redirectErrorStream(true).start()
		
		proc.inputStream.eachLine {
			sb.append(it)
		}
		
		proc.waitFor()
		
		def exitCode = proc.exitValue()
		return [exitCode, sb.toString()]
	}
	
	List buildCmd() {
		List cmd = [ucdClient]
		
		cmd.add("-weburl")
		cmd.add(webUrl)
		
		if (proxyHost?.trim()) {
			cmd.add("-proxyHost")
			cmd.add(proxyHost)
		}
		
		if (proxyPort?.trim()) {
			cmd.add("-proxyPort")
			cmd.add(proxyPort)
		}
		
		return cmd
	}
	
	/**
	 * Create a component version and upload files from the baseDir
	 */
	void addVersionFiles(String component, String versionName, File baseDir) {
		log.info("Add version files: component=$component, version=$versionName, baseDir=${baseDir.path}")
		def versionId = createVersion(component, versionName)
		def base = baseDir.getAbsoluteFile()
		
		List req = buildCmd()
		req.add("addVersionFiles")
		req.add("-version")
		req.add(versionId)
		req.add("-base")
		req.add(base)
		runCommand(req)
	}
	
	/**
	 * Set version properties on a component
	 * @param component component name
	 * @param props property map
	 * 
	 * Call createComponentVersionPropDefs() first, and then this method
	 */
	void setComponentVersionProperties(component, version, props) {
		props.each { key, value ->
			log.info("setVersionProperty -component $component -version $version -name $key -value $value")
			List req = buildCmd()
			req.add("setVersionProperty")
			req.add("-component")
			req.add(component)
			req.add("-version")
			req.add(version)
			req.add("-name")
			req.add(key)
			req.add("-value")
			req.add(value)
			runCommand(req)
		}
	}
	
	/**
	 * Create version property definitions of a component
	 * @param names: list of property names
	 */
	void createComponentVersionPropDefs(component, names) {
		log.info("Create componentVersionPropDefs component=$component, names=$names")
		def propDefs = getComponentVersionPropDefs(component)
		
		names.each {
			if (!findComponentVersionPropDef(propDefs, it)) {
				// set version property definition
				List req = buildCmd()
				req.add("setComponentVersionPropDef")
				req.add("-component")
				req.add(component)
				req.add("-name")
				req.add(it)
				
				log.info("setComponentVersionPropDef: -component $component -name $it")
				runCommand(req)
			}
		}
	}
	
	// query udeploy to get version property definitions of a component
	// and convert it to JSON
	def getComponentVersionPropDefs(component) {
		List req = buildCmd()
		req.add("getComponentVersionPropDefs")
		req.add("-component")
		req.add(component)
		
		log.debug("getComponentVersionPropDefs -component $component")
		def text = runCommand(req)
		log.debug("ComponentVersionPropDefs $text")
		text = preProcess(text)
		def result = jsonSlurper.parseText(text)
		return result
	}
	
	boolean findComponentVersionPropDef(propDefs, name) {
		boolean found = false
		
		propDefs.find { propDef ->
			if (propDef.name == name) {
				found = true
				return true
			}
		}
		
		return found
	}
	
	String runCliCmd(List params) {
		log.debug("Run Command: " + params.toString())
		
		List req = buildCmd()
		params.each {
			req.add(it)
		}
		
		String out = runCommand(req)
		log.debug("Run Out: $out")
		return out
	}
	
	// return versionID
	String createVersion(String component, String versionName, boolean allowVersionExist = true) {
		log.info("createVersion -component $component -name $versionName")
		List req = buildCmd()
		req.add("createVersion")
		req.add("-component")
		req.add(component)
		req.add("-name")
		req.add(versionName)
		
		def (exitCode, text) = runCommand2(req)
		if (log.isDebugEnabled()) {
			log.debug("exitCode: $exitCode")
			log.debug(text)
		}
		
		if (exitCode != 0) {
			if (!allowVersionExist) {
				throw new Exception("exitCode: $exitCode, error: $text")
			}			
			def found = (text =~ /(.*)Version with name $versionName already exists for Component#(.*) \((.*)\)/)			
			if (found.count > 0) {
				def versionId = found[0][2]
				return versionId
			} else {
				throw new Exception("exitCode: $exitCode, error: $text")
			}
		}
		
		text = preProcess(text)		
		def result = jsonSlurper.parseText(text)
		return result.id
	}
	
	String preProcess(String text) {		
		// Using 20080 for proxy port.Using 20080 for proxy port.
		// The above string is added in front when using a proxy
		int start = text.indexOf("{")
		if (start >= 0) {
			text = text.substring(start)
		}
		return text	
	}
		
	void login() {
		login(null, null)
	} 
	
	void login(String user, String password) {
		if (user == null) {
			user = conf.getConfig(UCD_USER)
		}
		
		if (password == null) {
			password = conf.getConfig(UCD_PASSWORD, true)
		}
		
		log.info("Login $webUrl ... with user $user")
		
		List req = buildCmd()
		req.add("-username")
		req.add(user)
		req.add("-password")
		req.add(password)
		req.add("login")
		runCommand(req)
		
		log.info("Login $webUrl SUCCEED")
		loggedIn = true
	}
	
	void logout() {
		if (loggedIn) {
			log.info("Logout $webUrl ...")
			List req = buildCmd()
			req.add("logout")
			runCommand(req)
			
			log.info("Logout $webUrl SUCCEED")
			loggedIn = false
		}
	}
}
