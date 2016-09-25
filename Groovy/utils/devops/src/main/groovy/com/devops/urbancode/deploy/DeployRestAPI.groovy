package com.devops.urbancode.deploy

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.protocol.HttpContext

import com.otpp.devops.utils.ConfigFile

/**
 * Use UrbanCode deploy REST API
 * @author 
 */
@Slf4j
class DeployRestAPI {
	static final String UCD_URL = 'urbancode.deploy.server'
	static final String UCD_PROXY_HOST = 'urbancode.deploy.proxyHost'
	static final String UCD_PROXY_PORT = 'urbancode.deploy.proxyPort'
	
	private File defaultKeyFile = new File(System.getProperty('user.home') + '/devops/conf/.devops')

	// config file
	ConfigFile conf
	
	String webUrl
	String proxyHost
	String proxyPort
	
	DeployRestAPI(File confFile, File keyFile = null) {
		if (keyFile == null) {
			keyFile = this.defaultKeyFile
		}

		conf = new ConfigFile(confFile, keyFile)
		init()
	}
	
	DeployRestAPI(ConfigFile uconfFile) {
		conf = uconfFile
		init()
	}
	
	private void init() {
		webUrl = conf.getConfig(UCD_URL)
		proxyHost = conf.getConfig(UCD_PROXY_HOST)
		proxyPort = conf.getConfig(UCD_PROXY_PORT)
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
			// set proxy
			assert proxyPort != null : "Please define proxyPort in devops.properties file"
			
			log.info("Login with proxy ${proxyHost}:${proxyPort}...")
			httpBuilder.setProxy(proxyHost, Integer.parseInt(proxyPort), null)
		}
		
		// login
		def postBody = [username: user, password: password]
		
		httpBuilder.post(path: '/tasks/LoginTasks/login', body: postBody,
			requestContentType: ContentType.URLENC) { resp ->
			
			log.info("Login SUCCEED to ${webUrl} with user ${user}")
		}
			
		return httpBuilder
	}
	
	void logout(HTTPBuilder httpBuilder) {
		httpBuilder.request(Method.GET, ContentType.TEXT) { req ->
			uri.path = '/tasks/LoginTasks/logout'
		 
			response.'401' = { resp, reader ->
				log.info("Logout SUCCEED from ${webUrl} SUCCEED")
			}
		 
			response.'200' = { resp, reader ->
				log.info("Logout SUCCEED from ${webUrl} SUCCEED")
			}

			response.failure = { resp, reader ->
				log.error("Logout failed: ${resp.statusLine}")
				log.error(reader.text)
				throw new Exception("Logout from ${webUrl} failed")
			}
		}
	}
	
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
		
		// process error
		return httpBuilder
	}
}
