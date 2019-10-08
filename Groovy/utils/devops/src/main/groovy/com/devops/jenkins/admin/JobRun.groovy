package com.devops.jenkins.admin
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class JobRun {
	@Parameter(names = "-U", description = "Jenkins user", required = true)
	String user
	@Parameter(names = "-P", description = "Jenkins password", required = true)
	String password
	@Parameter(names = "-URL", description = "Jenkins job URL", required = true)
	String url

	public void run() {
		def nullTrustManager = [
			checkClientTrusted: {chain, authType ->},
			checkServerTrusted: {chain, authType ->},
			getAcceptedIssuers: {
				null
			}
		]
		def nullHostnameVerifier = [
			verify: {hostname, session ->
				true
			}
		]
		SSLContext sc = SSLContext.getInstance("SSL")
		sc.init(null,[nullTrustManager as X509TrustManager] as TrustManager[], null)
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
		HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)
		def UserID = 'admin:admin'
		def authString = UserID.getBytes().encodeBase64().toString()
		def baseUrl = new URL('http://localhost:8080/job/Test/job/Whoami/build')
		def queryString = ''
		//def queryString = 'q=groovy&format=json&pretty=1&JenkinsParameter'

		def connection = baseUrl.openConnection()
		connection.setRequestProperty("Authorization", "Basic ${authString}")
		connection.setRequestProperty("Content-Type", "text/plain")
		connection.setRequestProperty("charset", "utf-8")
		connection.setRequestProperty("ignoreSSLCertsErrors", "true")
		connection.setRequestProperty("Accept", "*/*")
		connection.with {
			doOutput = true
			//isSSL = false
			requestMethod = 'POST'
			outputStream.withWriter { writer ->
				writer << queryString
			}
		}
		println "Response Code/Message: ${connection.responseCode}"
		// 403: the Jenkins URL is a http, not https
		//System.exit(0)
	}
}
