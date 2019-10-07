package com.devops.urbancode.admin

import groovy.util.logging.Slf4j

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

import org.codehaus.groovy.runtime.StackTraceUtils

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters

@Slf4j
@Parameters(commandDescription = "Upload webmethods deploy file to artifactory server, and update udeploy")
class WebmethodsUploader {
	@Parameter(names = "-VER", description = "artifact version", required = true)
	String version
	
	@Parameter(names = "-F", description = "deploy file", required = true)
	String fileName

	def verifyXml() {
		log.info("Validate webMethods config file $fileName")
		
		def xsdFile = this.getClass().getResource('/WebMethodsAssets.xsd')
		
		// 1. Lookup a factory for the W3C XML Schema language
		SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
		
		// The schema is loaded from a File		
		Schema schema = factory.newSchema(xsdFile)
	
		// 3. Get a validator from the schema.
		Validator validator = schema.newValidator()
		
		// 4. Parse the document you want to check.
		Source source = new StreamSource(fileName)
			
		// 5. Check the document
		validator.validate(source)
		
		def deployXml = new XmlParser().parse(new File(fileName))
		
		// validate SVN
		def svnType = 'TAG'
		
		if (deployXml.SVN_Type) {
			svnType = deployXml.SVN_Type.text()
		}

		if (svnType == 'TAG') {
			assert (deployXml.SVN_Tag != null) : "SVN_Tag is missing!"
		}
		
		return deployXml
	}		
	
	public void run() {
		def deployXml = verifyXml()

		ArtifactoryUpload aup = new ArtifactoryUpload()
		aup.artifactId = deployXml.'@AID'
		aup.groupId = deployXml.'@GID'
		aup.repo = deployXml.'@REPO'
		aup.version  = version
		aup.component  = deployXml.'@URBANCODECOMP'
		aup.path = fileName
		
		aup.process()
	}

	public static void main(String[] args) {
		WebmethodsUploader wmloader = new WebmethodsUploader()
		JCommander jcmd = new JCommander(wmloader)
		
		try {
			jcmd.parse(args)
			
			wmloader.run()
			System.exit(0)
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(0)
			
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("WebmethodsUploader FAILED", ta)
			System.exit(-1)
		}
	}
}
