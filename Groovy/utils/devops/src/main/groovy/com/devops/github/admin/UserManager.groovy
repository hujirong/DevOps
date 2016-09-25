package com.devops.github.admin

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.protocol.HttpContext
import org.codehaus.groovy.runtime.StackTraceUtils

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters
import com.devops.utils.ADUtilities
import com.devops.utils.ConfigFile
import com.devops.utils.ConfigManager
import com.devops.urbancode.admin.UdeployUpdate
import com.devops.urbancode.deploy.DeployClient
import groovy.json.JsonSlurper
import org.apache.directory.groovyldap.LDAP
import org.apache.directory.groovyldap.SearchScope


/**
 * Provide functions to manage GitHub users
 * 
 * @author 
 *
 */
@Slf4j
class UserManager {
	String user
	ConfigFile conf
	String baseUrl
	def cred
	ADUtilities adu = new ADUtilities()

	UserManager() {
		conf = ConfigManager.loadConf()
		cred = ConfigManager.getCredential(conf, user)
		baseUrl = conf.getConfig("github.rest.api")
	}

	// find all GitHub users no longer in AD
	def checkAD() {

		def txt = getUsers()

		def slurper = new JsonSlurper()
		def userList = slurper.parseText(txt)
		assert userList instanceof List
		def baduserList = []

		userList.each {
			//assert it instanceof Map
			//print it.login + " " + it.ldap_dn + "\n"
			//assert (connection.exists(it.ldap_dn)):" Person $it.ldap_dn does not exists"
			
			if (it.ldap_dn == null) {
				log.info ("$it.login dn is null, next\n")
				return
			}			

			if (!adu.exist(it.ldap_dn)) {
				log.info("$it.ldap_dn not exists\n")
				baduserList.push (it.ldap_dn)
			}			
		}
		log.info("\n")
		return baduserList

	}


	// Get list of users
	def getUsers() {
		String path = "http://github.otpp.com/api/v3/users"
		log.info("Retrieve all GitHub users")

		def httpBuilder = getHTTPBuilder()
		def result
		httpBuilder.request(Method.GET, ContentType.TEXT) { req ->
			uri.path = path
			headers."Accept" = 'application/json'

			response.success = { resp, reader ->
				log.info("Succeed: ${resp.statusLine}")
				result = reader.text
				//log.info("Result: $result")
			}

			response.failure = { resp, reader ->
				log.error("Failed: ${resp.statusLine}")
				log.error(reader.text)
				throw new Exception("Deploy artifact failed")
			}
		}
		return result;
	}

	// Suspend the user
	def suspendUser(String username) {
		String path = "http://github.otpp.com/api/v3/users/$username/suspended"
		log.info("Suspend GitHub users")

		def httpBuilder = getHTTPBuilder()
		def result
		httpBuilder.request(Method.PUT, ContentType.TEXT) { req ->
			uri.path = path
			headers."Accept" = 'application/json'
			//headers.'Content-Length' = 0

			response.success = { resp, reader ->
				log.info("Succeed: ${resp.statusLine}")
				//log.info(reader.text)
			}

			response.failure = { resp, reader ->
				log.error("Failed: ${resp.statusLine}")
				log.error(reader.text)
				throw new Exception("Suspend users failed")
			}
		}
		return result;
	}


	HTTPBuilder getHTTPBuilder() {
		def httpBuilder = new HTTPBuilder(baseUrl)
		httpBuilder.ignoreSSLIssues()

		// don't use auth.basic because it sends request twice and for PUT request
		// it causes error:
		//   Cannot retry request with a non-repeatable request entity
		//
		// restClient.auth.basic(user, password)

		httpBuilder.client.addRequestInterceptor(new HttpRequestInterceptor() {
					void process(HttpRequest httpRequest, HttpContext httpContext) {
						httpRequest.addHeader('Authorization', 'Basic ' + "${cred.user}:${cred.password}".bytes.encodeBase64().toString())
					}
				})

		// process error
		return httpBuilder
	}

	public static void main(String[] args) {
		UserManager aup = new UserManager()
		JCommander jcmd = new JCommander(aup)

		try {
			def badusers = aup.checkAD()			
			badusers.each {
				log.info("Suspend user: $it")
				//aup.suspendUser(it)
			}
			log.info("\n")
			//aup.getUsers()
			System.exit(0)
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(0)

		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("GithubUserManager FAILED", ta)
			System.exit(-1)
		}

		System.exit(0)
	}
}
