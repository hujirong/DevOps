package com.devops.urbancode.deploy

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

/**
 * Access UCD with undocumented REST API:
 * https://host:port/rest/deploy
 * 
 * @author 
 *
 */
@Slf4j
class DeployRestClient {
	private String webUrl
	
	private boolean loggedIn = false
	
	private cookies = [] as Set
	
	private HTTPBuilder httpBuilder

	DeployRestClient(String webUrl) {
		this.webUrl = webUrl
	}
	
	void login(String user, String password) {
		httpBuilder = new HTTPBuilder("$webUrl")
		httpBuilder.ignoreSSLIssues()

		def postBody = [ username: "$user",
			password: "$password" ]
		
		log.info("REST API login $webUrl with user $user")
		
		httpBuilder.request(Method.POST, ContentType.URLENC) { req ->
			uri.path = '/tasks/LoginTasks/login'
			body = postBody
			
			response.success = { resp, reader ->
				resp.getHeaders('Set-Cookie').each {
					it.value.split(';').each { cookie ->
						cookies.add(cookie.trim())
					}
				}
					
				log.info("Logged into $webUrl")
				System.out << reader
			}
		}
		
		loggedIn = true
	}
	
	private buildCookiesHeader() {
		return cookies.join(';')
	}
	
	public logout() {
		if (!loggedIn) {
			log.warn("Not logged in")
			return
		}
		
		log.info("REST API: Logout $webUrl ...")
		
		httpBuilder.request(Method.GET, ContentType.URLENC) { req ->
			uri.path = '/tasks/LoginTasks/logout'
			headers['Cookie'] = buildCookiesHeader()
			
			response.'401' = { resp ->
				log.debug("Logged out with 401 Unauthorized")
			}
			
			response.success = { resp, reader ->
				log.info("Logged out from $webUrl")
				System.out << reader
			}
		}

		loggedIn = false
		httpBuilder = null
		cookies.clear()
	}
	
	public String restCall(String restReq, Method method = Method.GET) {
		log.debug("REST API Call: $restReq, method=$method")

		String restResp
				
		httpBuilder.request(method, ContentType.TEXT) { req ->
			uri.path = restReq
			headers['Cookie'] = buildCookiesHeader()
			headers['Accept'] = '*/*'
			
			response.success = { resp, reader ->
				restResp = reader.text
			}
		}
		
		return restResp
	}
}
