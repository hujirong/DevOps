package com.devops.urbancode.admin

import groovy.util.logging.Slf4j

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import org.codehaus.groovy.runtime.StackTraceUtils

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters

@Slf4j
@Parameters(commandDescription = "Upload Python module zip file to artifactory server, and update UrbanCode")
class PythonUploader {
	@Parameter(names = "-F", description = "Python module zip file", required = true)
	String fileName

	@Parameter(names = "-REPO", description = "Artifactory Repository", required = true)
	String repo
	
	@Parameter(names = "-COMP", description = "UrbanCode component name. If not specified then do not update UrbanCode", required = false)
	String component

	String packageName = null
	String version = null
	
	def loadPackageInfo(InputStream instream) {
		instream.eachLine { line ->
			if (line.startsWith('Name:')) {
				packageName = line[line.indexOf(':') + 1 .. -1].trim()
			}
			
			else if (line.startsWith('Version:')) {
				version = line[line.indexOf(':') + 1 .. -1].trim()
			}
		}
		
		assert packageName : "Name is missing in PKG-INFO"
		assert version : "Version is missing in PKG-INFO"
	}
	
	def findPakcageInfo() {
		File f = new File(fileName)
		assert f.exists() : "File $fileName is not exist"
		
		boolean found = false
		
		ZipFile zipFile = new ZipFile(fileName)
		
		zipFile.entries().each { ZipEntry entry ->
			if (entry.getName().endsWith('PKG-INFO')) {
				log.info("Read ZipEntry ${entry.getName()}")
				loadPackageInfo(zipFile.getInputStream(entry))
				
				found = true
			}
		}
		
		if (!found) {
			throw new Exception("Cannot find PKG-INFO in file $fileName")
		}
	}

	void run() {
		findPakcageInfo()
		
		log.info("Find packageName: $packageName, version: $version")
		
		ArtifactoryUpload aup = new ArtifactoryUpload()
		aup.groupId = 'pypi'
		aup.artifactId = packageName
		aup.repo = repo
		aup.version  = version
		aup.component  = component
		aup.path = fileName
		
		aup.process()

	}

	public static void main(String[] args) {
		PythonUploader pyloader = new PythonUploader()
		JCommander jcmd = new JCommander(pyloader)
		
		try {
			jcmd.parse(args)
			
			pyloader.run()
			System.exit(0)
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(0)
			
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("PythonUploader FAILED", ta)
			System.exit(-1)
		}
	}
}
