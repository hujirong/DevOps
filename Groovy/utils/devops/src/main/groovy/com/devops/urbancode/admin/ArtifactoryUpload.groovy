package com.devops.urbancode.admin

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
import com.devops.urbancode.deploy.*
import com.devops.utils.ConfigFile

/**
 * Upload artifacts to artifactory server, and update metadata on udeploy:
 * 
 */
@Slf4j
@Parameters(commandDescription = "Upload artifacts to artifactory server, and update udeploy")
class ArtifactoryUpload {
	static String ARTIFACTORY_URL = "artifactory.server"
	
	@Parameter(names = "-REPO", description = "Artifactory Repository", required = true)
	String repo
	
	@Parameter(names = "-GID", description = "groupId", required = true)
	String groupId
	
	@Parameter(names = "-AID", description = "artifactId", required = true)
	String artifactId

	@Parameter(names = "-VER", description = "Component Version (same as artifact version)", required = true)
	String version
	
	@Parameter(names = "-COMP", description = "UDeploy Component Name", required = false)
	String component
	
	@Parameter(names = "-PATH", description = "Path contains all files to be uploaded to artifactory", required = true)
	String path

	@Parameter(names = "-COMMENT", description = "comments", required = false)
	String comments

	@Parameter(names = "-U", description = "User name, if not specified use urbancode.deploy.user", required = false)
	String user
	
	ConfigFile conf
	String baseUrl
	String groupIdPath
	
	def cred
		
	ArtifactoryUpload() {
	}
	
	void process() {
		conf = UdeployUpdate.loadConf()
		
		cred = UdeployUpdate.getCredential(conf, user)
		baseUrl = conf.getConfig(ARTIFACTORY_URL)
		
		log.info("Artifactory upload: baseUrl=$baseUrl")
		log.info("            user=${cred.user}")
		log.info("            repo=$repo, groupId=$groupId, artifactId=$artifactId, version=$version")
		log.info("            path=$path")
		
		if (component != null) {
			log.info("            udeploy.component=$component")
		}
		
		if (comments != null) {
			log.info("            comments=$comments")
		}

		groupIdPath = groupId.replace('.', '/')

		File f = new File(path)
		if (!f.exists()) {
			log.error("Path $path is not exist")
			return
		}
		
		deleteVersion()
		
		if (f.isFile()) {
			deployArtifact(f)
		} else if (f.isDirectory()) {
			deployArchiveArtifacts(f)
		}
		
		if (component?.trim()) {
			updateUdeploy()
		}
	}

	void updateUdeploy() {
		def udeploy = new UdeployUpdate()
		udeploy.version = version
		udeploy.component = component
		udeploy.conf = conf
		udeploy.user = user
		
		/*
		udeploy.repo = repo
		udeploy.groupId = groupId
		udeploy.artifactId = artifactId
		udeploy.comments = comments
		*/
		
		udeploy.process()
	}
	
	// delete artifact version
	void deleteVersion() {
		// check if folder exist
		String artifactPath = "/artifactory/api/storage/$repo/$groupIdPath/$artifactId/$version"
		log.info("Check if path exist: $artifactPath")
		
		def foundFolder = false
		
		def httpBuilder = getHTTPBuilder()

		httpBuilder.request(Method.GET, ContentType.TEXT) { req ->
			uri.path = artifactPath
			headers.Accept = ContentType.JSON.getAcceptHeader()
			
			response.'404' = { resp, reader ->
				foundFolder = true
			}
		 
			response.failure = { resp, reader ->
				log.error("Failed: ${resp.statusLine}")
				log.error("    ${reader.text}")
				throw new Exception("Get artifactory path failed")
			}
		}

		if (foundFolder) {
			return
		}
		
		// delete folder
		artifactPath = "/artifactory/$repo/$groupIdPath/$artifactId/$version"
		log.info("Delete path $artifactPath")
		
		httpBuilder = getHTTPBuilder()

		httpBuilder.request(Method.DELETE) { req ->
			uri.path = artifactPath
		 
			response.failure = { resp, reader ->
				log.error("Failed: ${resp.statusLine}")
				log.error(reader.text)
				throw new Exception("Delete artifactory path failed")
			}
		}
	}
	
	// deploy single artifact
	void deployArtifact(File file) {
		String artifactPath = "/artifactory/$repo/$groupIdPath/$artifactId/$version/" + file.getName()
		log.info("Upload single file to $artifactPath")
		
		def httpBuilder = getHTTPBuilder()

		httpBuilder.request(Method.PUT, ContentType.TEXT) { req ->
			uri.path = artifactPath
			send(ContentType.BINARY, file.bytes)
		 
			response.'201' = { resp, reader ->
				log.info("Succeed: ${resp.statusLine}")
				log.info(reader.text)
			}
		 
			response.success = { resp, reader ->
				log.info("Succeed: ${resp.statusLine}")
				log.info(reader.text)
			}
		 
			response.failure = { resp, reader ->
				log.error("Failed: ${resp.statusLine}")
				log.error(reader.text)
				throw new Exception("Deploy artifact failed")
			}
		}
	}
	
	// deploy archived artifacts
	void deployArchiveArtifacts(File dir) {
		// zip files
		def file = new File(System.getProperty('user.home') + '/devops/tmp')
		file.mkdirs()
		file = new File(file, 'artifactoryUpload.zip')
		
		// don't include sha1, md5 files, artifactory will generate its own checksum files.
		def ant = new AntBuilder()
		ant.zip(destfile: file, basedir: dir.path, excludes: "**/*.sha1, **/*.md5")
		
		String artifactPath = "/artifactory/$repo/$groupIdPath/$artifactId/$version/" + file.getName()
		log.info("Upload archive to $artifactPath");

		def httpBuilder = getHTTPBuilder()
		
		httpBuilder.request(Method.PUT, ContentType.TEXT) { req ->
			uri.path = artifactPath
			headers.'X-Explode-Archive' = 'true'
			
			send(ContentType.BINARY, file.bytes)
				 
			response.'201' = { resp, reader ->
				log.info("Succeed: ${resp.statusLine}")
			}
				 
			response.success = { resp, reader ->
				log.info("Succeed: ${resp.statusLine}")
			}
				 
			response.failure = { resp, reader ->
				log.error("Failed: ${resp.statusLine}")
				log.error(reader.text)
				throw new Exception("Deploy archive artifacts failed")
			}
		}
		
		file.delete()
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
		ArtifactoryUpload aup = new ArtifactoryUpload()
		JCommander jcmd = new JCommander(aup)
		
		try {
			jcmd.parse(args)
			
			aup.process()
			System.exit(0)
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(0)
			
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("ArtifactoryUpload FAILED", ta)
			System.exit(-1)
		}
		
		System.exit(0)
	}
}
