
class Common {
	private static String UCD_PATH = '/var/lib/ibm-ucd'

	static {
		def ucdPath = System.properties.'UCD_PATH'
		if (ucdPath) {
			UCD_PATH = ucdPath
		}
	}
	
	static String JOBS_PATH = '/jobs'
	static String DEPLOY_ENVS_FILE = '/bin/deploy-envs.properties'
	
	static String ABE_PATH = '/ABE-repo'
	static String WORKSPACE_PATH = '/workspace'
	
	static String WMDEPLOY_CONF_FILE = 'wmdeploy_conf.properties'
	
	static String getUcdPath() {
		return UCD_PATH
	}
	
	static String getJobsPath() {
		return UCD_PATH + JOBS_PATH
	}
	
	static String getDeployEnvsFile() {
		return UCD_PATH + DEPLOY_ENVS_FILE
	}
	
	static String getABERepoPath() {
		return UCD_PATH + ABE_PATH
	}
	
	static String getWorkspacePath() {
		return UCD_PATH + WORKSPACE_PATH
	}
	
	static String IS_POLLING_FILE = 'wmdeploy_polling.xml'

	static String IS_PROJECT_FILE = 'wmProjectIS.xml'

	// CAF has multiple project files
	static String CAF_PROJECT = 'wmProjectCAF'
	
	static String BPM_PROJECT_FILE = 'wmProjectBPM.xml'
	
	// project-automator does not like '-' in element names
	// convert '-' to '_'
	static String getProjectName(String appName, String envName) {
		String name = appName + '.' + envName
		name = name.replace('-', '_')
		return name.replace(' ', '_')
	}
	
	static String getFileNameCAF(String projectName) {
		String fname = CAF_PROJECT + '_' + projectName
		return fname.replace(' ', '_')
	}
	
	static String convertAppName(String appName) {
		String name = appName.replace('-', '_')
		name = name.replace(' ', '_')
		name = name.toLowerCase()
	}
	
	static void verifyDeployReport(File reportFile) {
		if (!reportFile.exists()) {
			println "Report file $reportFile is not exist"
			return
		}
		
		println "Verify report file: $reportFile"
		def report = new XmlParser().parse(reportFile)
		
		boolean succeed = true
		
		report.messages.message.each { msg ->
			if (msg.'@type' == 'ERROR') {
				println "===> Deploy ERROR"
				XmlNodePrinter xmlPrinter = new XmlNodePrinter()
				xmlPrinter.print(msg)
				println '\n'
				
				succeed = false
			}
		}
		
		if (!succeed) {
			System.exit(-8)
		}
	}
	
	// a wrapper closure around executing a string
	// can take either a string or a list of strings (for arguments with spaces)
	// prints all output, complains and halts on error
	static runCommand = { strList ->
	  assert (strList instanceof String ||
			 (strList instanceof List && strList.each { it instanceof String } ))

	  def proc = strList.execute()
	  proc.in.eachLine { line -> println line }
	  proc.out.close()
	  proc.waitFor()
	
	  if (proc.exitValue()) {
		  print "Run command failed: "
		  if(strList instanceof List) {
			  strList.each { print "${it} " }
		  } else {
		  	print strList
		  }
	
		  println "[ERROR] ${proc.getErrorStream()}"
	  }
	  
	  assert !proc.exitValue()
	}
}
