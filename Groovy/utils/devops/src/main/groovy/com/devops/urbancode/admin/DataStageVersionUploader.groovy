package com.devops.urbancode.admin

import groovy.util.logging.Slf4j
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.*

import org.codehaus.groovy.runtime.StackTraceUtils

import com.aestasit.ssh.DefaultSsh
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters
import com.devops.utils.ConfigFile

/**
 * Upload DataStage version files
 * @author jimliu
 *
 */
@Slf4j
@Parameters(commandDescription = "Upload DataStage deploy file to artifactory server, and update udeploy")
class DataStageVersionUploader {
	
	private static String PACKAGE_ROOT = '/istoolsBuild'
	
	@Parameter(names = "-VER", description = "artifact version", required = true)
	String version
	
	@Parameter(names = "-F", description = "deploy file", required = true)
	String fileName
	
	private File workDir = new File(System.getProperty('user.home') +  '/devops/DataStage/upload')
	
	private ConfigFile conf
	
	ArtifactoryUpload artifactoryUpload
	
	private File deployConfFile
		
	private Node project
	
	def run() {
		deployConfFile = new File(fileName)
		
		log.info("Parse deploy configure file $deployConfFile")
		def xsdFile = this.getClass().getResource('/dsbuild.xsd')		
		verifyXml(xsdFile)		
		log.info("$deployConfFile is valid.");		
		project = new XmlParser().parse(deployConfFile)
				
		workDir.mkdirs()
		conf = UdeployUpdate.loadConf()

		uploadVersion()
	}
	
	private void verifyXml(xsdFile) {
		
		// 1. Lookup a factory for the W3C XML Schema language
		SchemaFactory factory =
			SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		
		// The schema is loaded from a File		
		Schema schema = factory.newSchema(xsdFile);
	
		// 3. Get a validator from the schema.
		Validator validator = schema.newValidator();
		
		// 4. Parse the document you want to check.
		Source source = new StreamSource(deployConfFile);
		// 5. Check the document
		validator.validate(source);		
		
		//6. Make sure no empty value
		Node chkDSConfFile = new XmlParser().parse(deployConfFile)
		def chkscript = chkDSConfFile.SCRIPTS.SCRIPT
		if  (chkscript.size() > 0) {
			chkscript.each {
				 String strScript=it.text()
				 if (strScript.trim().length() == 0) {
					 throw new Exception("Empty value in SCRIPT element")					 
				 }				      
			}
		}			
		def chkpkg = chkDSConfFile.PACKAGES.PACKAGE
		if  (chkpkg.size() > 0) {
			chkpkg.each {
				 String strPkg=it.text()
				 if (strPkg.trim().length() == 0) {
					 throw new Exception("Empty value in PACKAGE element")
				 }
			}
		}
	}	
	
	void uploadVersion() {
		log.info("Build version files at $workDir")
		buildFiles()

		def  atf_aid = project.'@AID'
		def  atf_gid = project.'@GID'
		def  atf_repo = project.'@REPO'
		def  atf_ver  = version
		def  uc_comp  = project.'@URBANCODECOMP'
					
		ArtifactoryUpload aup = new ArtifactoryUpload()
		aup.repo = atf_repo
		aup.groupId = atf_gid
		aup.version = atf_ver
		aup.artifactId = atf_aid
		aup.path = workDir
		aup.component = uc_comp
		
		aup.process()
			
	}
	
	private void buildFiles() {
		def ant = new AntBuilder()
		ant.delete(includeemptydirs: true) {
			fileset(dir: workDir, includes: '**/*')
		}

		def deployConfFileName=deployConfFile.getName() 
		// copy project File
		ant.copy(file: deployConfFile, tofile: "$workDir/$deployConfFileName")
		
		// use sshoogr to download package files, script files
		def srcServer = conf.getConfig('datastage.server')
		def srcUser = conf.getConfig('datastage.user')
		def srcPassword = conf.getConfig('datastage.password', true)
						
		def packageRoot = PACKAGE_ROOT
		def sshOutput = Boolean.valueOf(conf.getConfig('datastage.ssh.output'))
		
		DefaultSsh.options.with {
			execOptions.with {
				showOutput = sshOutput
				showCommand = sshOutput
			}
		}
		
		DefaultSsh.remoteSession {
			user = srcUser
			password = srcPassword
			host = srcServer
			connect()
			// download package files
			//
			log.info("Download package files from $srcServer")
			project.PACKAGES.PACKAGE.each {
				def pkgFile = it.'@FOLDER' + '/' + it.text() + '.pkg'
								
				scp {
					from { remoteFile("$packageRoot/$pkgFile") }
					into { localDir("$workDir") }					
				}
			}
			
			// tar scripts and donwload
			def script = project.SCRIPTS.SCRIPT
			if (script.size() > 0) {
				def files = ""
				
				script.each {
					files = files + it.text() + ' '
				}
				
				log.debug("Tar script files $files")
				
				def tmpDir = "/DSHOME/$srcUser/devops/workdir"
				def destFile = "scripts.tar"
				
				def baseDir = project.'@ROOTPATH'

				exec "mkdir -p $tmpDir"
				exec "rm -f ${tmpDir}/*"
				
				exec "tar -cz --file=${tmpDir}/${destFile} -C ${baseDir} ${files}"

				scp {
					from { remoteFile("$tmpDir/$destFile") }
					into { localDir("$workDir") }
				}								
			}
		}
	}
			
	public static void main(String[] args) {
		DataStageVersionUploader uploader = new DataStageVersionUploader()
		JCommander jcmd = new JCommander(uploader)
		
		try {
			jcmd.parse(args)
			uploader.run()
			System.exit(0)
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(0)
			
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("DataStageVersionUploader FAILED", ta)
			System.exit(-1)
		}
	}
}
