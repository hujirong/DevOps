package com.devops.jira

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.protocol.HttpContext

import com.devops.utils.ConfigFile

/**
 * Create a HttpBuilder to be used for JIRA REST API
 * Use defalt config file in the default user profile
 */

@Slf4j
class JIRARestAPI {
	
	static String KEY_FILE = '.devops'
	private File defaultKeyFile = new File(System.getProperty('user.home') + '/devops/conf/.devops')
	
	static final String JIRA_URL = 'jira.server'
	static final String JIRA_USER = 'jira.user'
	static final String JIRA_PASSWORD = 'jira.password'
	static final String JIRA_PROXY_HOST = 'jira.proxyHost'
	static final String JIRA_PROXY_PORT = 'jira.deploy.proxyPort'
	
	ConfigFile configFile
	String webUrl
	String proxyHost
	String proxyPort
	
	private ConfigFile loadConf() {
		File confDir = new File(System.getProperty('user.home') +  '/devops/conf')
		File keyFile = new File(confDir, KEY_FILE)
		if (!keyFile.exists()) {
			log.info("key file $keyFile is not exist")
		}
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
			user = conf.getConfig(JIRARestAPI.JIRA_USER)
			password = conf.getConfig(JIRARestAPI.JIRA_PASSWORD, true)
		}		
		return [user: user, password: password]
	}
	
	JIRARestAPI() {		
		configFile = loadConf()  // load config file from default user
		webUrl = configFile.getConfig(JIRA_URL)
		proxyHost = configFile.getConfig(JIRA_PROXY_HOST)
		proxyPort = configFile.getConfig(JIRA_PROXY_PORT)
	}
	
	HTTPBuilder login(String user, String password) {
		// user cannot by empty
		assert user?.trim()		
		if (!password?.trim()) {
			password = conf.getConfig(user + '.password', true)
		}		
		assert password?.trim()
		
		def httpBuilder = new HTTPBuilder(webUrl)
		httpBuilder.ignoreSSLIssues()
		
		if (proxyHost?.trim()) {
			assert proxyPort != null : "Please define proxyPort in devops.properties file"
			log.info("Login with proxy ${proxyHost}:${proxyPort}...")
			httpBuilder.setProxy(proxyHost, Integer.parseInt(proxyPort), null)
		}
		
		// login
		def postBody = [username: user, password: password]		
		httpBuilder.post(path: '/rest/auth/1/session', body: postBody,
			requestContentType: ContentType.URLENC) { resp ->			
			log.info("Login SUCCEED to ${webUrl} with user ${user}")
		}
			
		return httpBuilder
	}
	
	
	HTTPBuilder getHTTPBuilder() {	
		return getHTTPBuilder(configFile.getConfig(JIRA_USER),configFile.getConfig(JIRA_PASSWORD))
	}
	
	// login with basic auth: https://docs.atlassian.com/jira-software/REST/7.0.4/
	HTTPBuilder getHTTPBuilder(String user, String password) {
		// user cannot by empty
		assert user?.trim()		
		if (!password?.trim()) {
			password = conf.getConfig(user + '.password', true)
		}		
		assert password?.trim()
		
		def httpBuilder = new HTTPBuilder(webUrl)
		httpBuilder.ignoreSSLIssues()
		
		if (proxyHost?.trim()) {
			// set proxy
			httpBuilder.setProxy(proxyHost, proxyPort, "https")
		}
		
		// don't use auth.basic because it sends request twice and for PUT request
		// it causes error:
		//   Cannot retry request with a non-repeatable request entity
		//
		// restClient.auth.basic(user, password)

		httpBuilder.client.addRequestInterceptor(new HttpRequestInterceptor() {
			void process(HttpRequest httpRequest, HttpContext httpContext) {
				httpRequest.addHeader('Authorization', 'Basic ' + "$user:$password".bytes.encodeBase64().toString())
			}
		})
		log.info("Get HttpBuilder SUCCEED to ${webUrl} with user ${user}")
		// process error
		return httpBuilder
	}
}

