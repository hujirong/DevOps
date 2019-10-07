package com.devops.github.admin

import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils
import com.devops.utils.ConfigFile
import com.devops.utils.ConfigManager 
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.UserService

import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.egit.github.core.service.OrganizationService
import org.eclipse.egit.github.core.Repository

@Slf4j
class ReOrg {
	String user
	ConfigFile conf
	String baseUrl
	def cred

	ReOrg (){
		conf = ConfigManager.loadConf()
		cred = ConfigManager.getCredential(conf, user)
	}

	def getOrgs (String user, String password) {
		def GitHubClient client
		client = CreateClient (user, password)
		OrganizationService service = new OrganizationService(client);
		return service.getOrganizations()
	}

	def CreateClient (String user, String password) {
		// Default to https, use alternative constructor
		// GitHubClient client = new GitHubClient("github.otpp.com");
		GitHubClient client = new GitHubClient("github.otpp.com", -1, "http");
		client.setCredentials(user, password);
		
		UserService service = new UserService(client)
		//log.info (service.getUser(user).getLogin())
		assert (user == service.getUser(user).getLogin()) : "client is bad"
		return client
	}

	def CreateClient (String token) {
		GitHubClient client = new GitHubClient("github.otpp.com", -1, "http");
		client.setOAuth2Token(token)
		return client
	}

	
	public static void main(String[] args) {
		ReOrg ro = new ReOrg()

		try {
			def orgs, user, password, token
			user = "huj"
			password = "Savit5ch"
			token = '0c18f61c0361e6bc5fc8dfbf6384383791f29692'
			orgs = ro.getOrgs(user, password)
			log.info("$user 's orgs: $orgs")

			System.exit(0)
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("ReOrg FAILED", ta)
			System.exit(-1)
		}
		System.exit(0)
	}
}