import groovy.xml.MarkupBuilder
import groovy.xml.MarkupBuilderHelper

class PreDeploy {
	def antBuilder = new AntBuilder()
	
	String outPropsFile
	
	Properties props = new Properties()
	
	// output properties
	Properties outProps = new Properties()

	// deploy-envs.properties
	Properties envProps = new Properties()
	
	// wmdeployer credentials
	Properties wmdeployProps = new Properties()
	
	String appName
	String envName
	
	String projectName
	
	String appName2
	
	// deploy jobDir saves deploy files: conf, generated meta-data file, 
	// and wmdeployer log files
	String jobDir
	
	// list all deploy types
	List deployTypes = []
	
	// ABE repo dir: ABE build webMethods assets to this directory
	File abeRepoDir
	
	// SVN workspace
	File workspaceDir
	
	def init(String[] args) {
		outPropsFile = args[1]
		
		// load input properties
		//
		new File(args[0]).withInputStream {
			stream -> props.load(stream)
		}
		
		// load envs properties
		new File(Common.getDeployEnvsFile()).withInputStream {
			stream -> envProps.load(stream)
		}
		
		// load wmdeployProps
		new File("${Common.UCD_PATH}/bin/deploymentCredentials.cnf").withInputStream {
			stream -> wmdeployProps.load(stream)
		}
		
		// deploy log path
		appName = props['appName']
		appName2 = Common.convertAppName(appName)
		envName = props['envName']
		
		projectName = Common.getProjectName(appName, envName)
		
		// build jobDir
		def date = new Date()
		date = date.format('yyyy-MM-dd_HH-mm-ss')
		
		jobDir = Common.getJobsPath() + "/$appName/$envName/$date"
		println "JOB_DIR is $jobDir"
		new File(jobDir).mkdirs()
		
		// build ABE repoDir
		abeRepoDir = new File(Common.getABERepoPath(), "/$appName/$envName")
		abeRepoDir.mkdirs()
		
		// build workspaceDir
		workspaceDir = new File(Common.getWorkspacePath(), "/$appName/$envName")
		workspaceDir.mkdirs()
	}
	
	def prebuild() {
		// copy conf file to deploy path
		def confFile = props['confFile']
		def fname = new File(confFile).getName()		
		
		println "Copy file $fname"
		antBuilder.copy(file: confFile, tofile: new File(jobDir, fname))
		
		// load conf file
		println "Load deploy configuration file: $confFile"
		def wmassets = new XmlParser().parse(new File(confFile))
		
		println "Set deploy properties"
		setProps(wmassets)
		saveProps()
	}
	
	def setProps(wmassets) {
		outProps.APP_NAME = appName
		outProps.ENV_NAME = envName
		outProps.PROJECT_NAME = projectName
		outProps.STAGE = props['stage']
		
		outProps.JOB_DIR = jobDir
		outProps.ABE_REPO_DIR = abeRepoDir.getPath()
		outProps.WORKSPACE_DIR = workspaceDir.getPath()
		outProps.DEPLOY_PROPS_FILE = jobDir + '/' + Common.WMDEPLOY_CONF_FILE 

		outProps.IS_target = envProps['IS_' + envName]
		outProps.IS_servers = envProps[envName]
		
		setSVN(wmassets)

		// set deploy types
		outProps.DEPLOY_IS = 'false'
		outProps.DEPLOY_BPM = 'false'
		outProps.DEPLOY_CAF = 'false'
		outProps.DEPLOY_BROKER = 'false'
		
		outProps.MN_POLLING = 'NA'
		
		// NO CAF projects
		outProps.CAF_PROJECTS = 'NA'
		
		if (wmassets.IntegrationServerAssets) {
			setISAssets(wmassets.IntegrationServerAssets[0])
		}
		
		if (wmassets.BPMAssets) {
			setBPMAssets(wmassets.BPMAssets[0])
		}
		
		if (wmassets.CAFAssets) {
			setCAFAssets(wmassets.CAFAssets[0])
		}
		
		if (wmassets.BrokerAssets) {
			setBrokerAssets(wmassets.BrokerAssets[0])
		}
		
		outProps.DEPLOY_TYPES = deployTypes.join(' ')
	}
	
	def setSVN(wmassets) {
		if (wmassets.SVN_Type) {
			outProps.SVN_TYPE = wmassets.SVN_Type.text()
			assert (outProps.SVN_TYPE == 'TRUNK' || outProps.SVN_TYPE == 'TAG') : "SVN_Type must be TRUNK or TAG"
			outProps.SVN_TAG = ''
		} else {
			outProps.SVN_TYPE = 'TAG'
		}

		if (outProps.SVN_TYPE == 'TAG') {
			outProps.SVN_TAG = wmassets.SVN_Tag.text()
			assert (outProps.SVN_TAG != null) : "SVN_Tag is missing!"
		}
		
		if (outProps.SVN_TYPE == 'TRUNK') {
			outProps.SVN_URL = envProps['SVN_TRUNK_' + appName2]
			assert (outProps.SVN_URL?.trim()) : "Cannot find SVN TRUNK URL for group $appName2"
		} else if (outProps.SVN_TYPE == 'TAG') {
			def url = envProps['SVN_TAG_' + appName2]
			assert (url?.trim()) : "Cannot find SVN TAG URL for group $appName2"
			outProps.SVN_URL = url  + '/' + outProps.SVN_TAG 
		}
		
		println "Set SVN_URL ${outProps.SVN_URL}"
	}
	
	def setISAssets(isAssets) {
		println 'Set IS properties'
		outProps.DEPLOY_IS = 'true'
		outProps.IS_PROJECT_FILE = jobDir + '/' + Common.IS_PROJECT_FILE
		outProps.PROJECT_IS = projectName + '_IS'
		
		if (isAssets.'@fullDeploy') {
			outProps.IS_fullDeploy = isAssets.'@fullDeploy'
			deployTypes.add('IS-Full')
		} else {
			deployTypes.add('IS')
		}
		
		if (outProps.IS_fullDeploy) {
			outProps.IS_candidate = 'Candidate.full.' + projectName
		} else {
			outProps.IS_candidate = 'Candidate.' + projectName
		}
		
		// packages
		def li = []
		isAssets.Packages.ISPackage.each {
			String pkg = it.'@name'
			validateISPackage(pkg)
			li.add(pkg)
		}
		
		if (li.size() > 0) {
			outProps.IS_packages = li.join(',')
		}
		
		// test suites
		li.clear()
		isAssets.TestSuites.TestSuite.each {
			li.add(it.'@path')
		}
		
		if (li.size() > 0) {
			outProps.IS_TestSuites = li.join(',')
		}
		
		// maintenance packages
		if (isAssets.MaintenancePackages) {
			writeISPolling(isAssets)
		} else {
			assert (props.stage != 'MNSTART') : "Stage is MNSTART but deploy config does not contain MaintenancePackages"
			assert (props.stage != 'MNEND') : "Stage is MNEND but deploy config does not contain MaintenancePackages"
			outProps.MN_POLLING = 'NA'
		}
	}
	
	void validateISPackage(String pkgName) {
		def prefix = envProps['IS_Package_' + appName2]
		
		def found = false
		prefix.split(',').find {
			if (pkgName.startsWith(it.trim())) {
				found = true
				return true
			}
		}
		
		if (!found) {
			throw new Exception("Invalid package name $pkgName")
		}
	}
	
	def writeISPolling(isAssets) {
		def mnfile = jobDir + '/' + Common.IS_POLLING_FILE
		println "Create IS maintenance config file $mnfile"
		
		outProps.MN_POLLING = mnfile
		
		Writer writer = new FileWriter(new File(mnfile))
		MarkupBuilder xmlBuilder = new MarkupBuilder(writer)

		xmlBuilder.ISMaintenance(controlJMS: true) {
			PackageList {
				isAssets.MaintenancePackages.ISPackage.each {
					def pkg = it.'@name'
					validateISPackage(pkg)
					ISPackage(name: pkg)
				}
			}
		}		

		writer.close()
	}
	
	def setBPMAssets(bpmAssets) {
		println 'Set BPM properties'
		outProps.DEPLOY_BPM = 'true'
		
		deployTypes.add('BPM')
		
		// generate BPM project file (used by wmdeployer)
		File projectFile = new File(jobDir, Common.BPM_PROJECT_FILE)
		outProps.BPM_PROJECT_FILE = projectFile.getPath()
		
		outProps.BPM_CANDIDATE = "Candidate.BPM." + projectName
		outProps.PROJECT_BPM = projectName + '_BPM'
		
		println "Create BPM project file ${outProps.BPM_PROJECT_FILE}"
		
		Writer writer = new FileWriter(projectFile)
		MarkupBuilder xmlBuilder = new MarkupBuilder(writer)
		
		def helper = new MarkupBuilderHelper(xmlBuilder)
		helper.xmlDeclaration([version:'1.0', encoding:'UTF-8', standalone:'no'])
		
		xmlBuilder.DeployerSpec(exitOnError: false, sourceType: 'Repository') {
			DeployerServer {
				host(wmdeployProps.host + ':' + wmdeployProps.port)
				user(wmdeployProps.user)
				pwd(wmdeployProps.pwd)
			}
			
			Environment {
				Repository {
					repalias(name: "Repo.${projectName}") {
						type('FlatFile')
						urlOrDirectory(abeRepoDir.getPath())
						Test('true')
					}
				}
			}
			
			Projects {
				Project(description: "$projectName BPM Deployment", ignoreMissingDependencies: 'true', 
					name: outProps.PROJECT_BPM, overwrite: 'true', type: 'Repository') {
					
					DeploymentSet(autoResolve: 'ignore', description: "$projectName BPM DeploymentSet",
						name: "Set.BPM.${projectName}", srcAlias: "Repo.${projectName}") {
						
						bpmAssets.BPMProcesses.BPMProcess.each {
							def pname = it.'@name'
							Composite(name: pname, srcAlias: "Repo.${projectName}", 
								type: 'BPM', displayName: "${projectName}/BPM/${pname}")
						}
					}
						
					DeploymentMap(name: "Map.BPM.${projectName}", description: "$projectName BPM DeploymentMap")
					
					MapSetMapping(mapName: "Map.BPM.${projectName}", setName: "Set.BPM.${projectName}") {
						// BPM target
						alias(envProps['BPM_' + envName], type: 'ProcessModel')
					}
					
					DeploymentCandidate(name: outProps.BPM_CANDIDATE, mapName: "Map.BPM.${projectName}", description: "$projectName candidate")
				}
			}
		}		

		writer.close()
	}
	
	def setCAFAssets(cafAssets) {
		println 'Set CAF properties'
		outProps.DEPLOY_CAF = 'true'
		outProps.PROJECT_CAF = projectName + '_CAF'
		outProps.CAF_CANDIDATE = "Candidate.MWS." + projectName
		
		deployTypes.add('CAF')

		// CAF need to be multiple projects
		def projects = []
		
		cafAssets.CAFProjects.CAFProject.each {
			buildProjectCAF(it, projects)
		}
		
		outProps.CAF_PROJECTS = projects.join(',')
	}
	
	// generate CAF project file (used by wmdeployer)
	def buildProjectCAF(cafProject, List projects) {
		String fname = Common.getFileNameCAF(cafProject.'@name')
		projects.add(fname)

		File projectFile = new File(jobDir, fname)
		println "Create CAF project file ${projectFile.getPath()}"
		
		Writer writer = new FileWriter(projectFile)
		MarkupBuilder xmlBuilder = new MarkupBuilder(writer)
		
		def helper = new MarkupBuilderHelper(xmlBuilder)
		helper.xmlDeclaration([version:'1.0', encoding:'UTF-8', standalone:'no'])
		
		xmlBuilder.DeployerSpec(exitOnError: false, sourceType: 'Repository') {
			DeployerServer {
				host(wmdeployProps.host + ':' + wmdeployProps.port)
				user(wmdeployProps.user)
				pwd(wmdeployProps.pwd)
			}
			
			Environment {
				Repository {
					repalias(name: "Repo.${projectName}") {
						type('FlatFile')
						urlOrDirectory(abeRepoDir.getPath())
						Test('true')
					}
				}
			}
			
			Projects {
				Project(description: "$projectName CAF Deployment", ignoreMissingDependencies: 'true', 
					name: outProps.PROJECT_CAF, overwrite: 'true', type: 'Repository') {
					
					DeploymentSet(autoResolve: 'ignore', description: "$projectName MWS DeploymentSet",
						name: "Set.MWS.${projectName}", srcAlias: "Repo.${projectName}") {
						
					def pname = cafProject.'@name'
					Composite(name: pname, srcAlias: "Repo.${projectName}", 
						type: 'MWS', displayName: "${projectName}/MWS/${pname}")
					}
						
					DeploymentMap(name: "Map.MWS.${projectName}", description: "$projectName MWS DeploymentMap")
					
					MapSetMapping(mapName: "Map.MWS.${projectName}", setName: "Set.MWS.${projectName}") {
						// BPM target
						alias(envProps['MWS_' + envName], type: 'MWS')
					}
					
					DeploymentCandidate(name: outProps.CAF_CANDIDATE, mapName: "Map.MWS.${projectName}", description: "$projectName candidate")
				}
			}
		}		

		writer.close()
	}
	
	def setBrokerAssets(brokerAssets) {
		println 'Set Broker properties'
		outProps.DEPLOY_BROKER = 'true'
		
		outProps.BROKER_SRC = workspaceDir.getPath() + '/Broker/dev-broker.xml'
		
		deployTypes.add('Broker')
		
		// JMSTopicConnectionFactory
		def li = []
		brokerAssets.TopicConnectionFactories.JMSTopicConnectionFactory.each {
			def name = it.'@name'
			validateJMS(name)
			li.add(name)
		}
		
		if (li.size() > 0) {
			outProps.BROKER_TCF_LIST = li.join(',')
		}
		
		// JMSTopicConnectionFactory
		li.clear()
		brokerAssets.QueueConnectionFactories.JMSQueueConnectionFactory.each {
			def name = it.'@name'
			validateJMS(name)
			li.add(name)
		}
		
		if (li.size() > 0) {
			outProps.BROKER_QCF_LIST = li.join(',')
		}
		
		// JMSTopic
		li.clear()
		brokerAssets.Topics.JMSTopic.each {
			def name = it.'@name'
			validateJMS(name)
			li.add(name)
		}
		
		if (li.size() > 0) {
			outProps.BROKER_TOPIC_LIST = li.join(',')
		}
		
		// JMSQueue
		li.clear()
		brokerAssets.Queues.JMSQueue.each {
			def name = it.'@name'
			validateJMS(name)
			li.add(name)
		}
		
		if (li.size() > 0) {
			outProps.BROKER_QUEUE_LIST = li.join(',')
		}
	}
	
	void validateJMS(String name) {
		def prefix = envProps['IS_JMS_' + appName2]
		
		def found = false
		prefix.split(',').find {
			if (name.startsWith(it.trim())) {
				found = true
				return true
			}
		}
		
		if (!found) {
			throw new Exception("Invalid JMS asset name $name")
		}
	}
	
	def saveProps() {
		println "Copy file $outPropsFile"
		
		// save output properties
		def outStream = new FileOutputStream(outPropsFile)
		outProps.store(outStream, "")
		outStream.close()
		
		// copy it to JOB_DIR
		antBuilder.copy(file: outPropsFile, tofile: outProps.DEPLOY_PROPS_FILE)
	}
	
	public static void main(String[] args) {
		if (args.length != 2) {
			println "Usage: PreDeploy.groovy inputPropsFile outputProsFile"
			System.exit(1)
		}
		
		PreDeploy pdeploy = new PreDeploy()
		pdeploy.init(args)
		pdeploy.prebuild()
	}
}
