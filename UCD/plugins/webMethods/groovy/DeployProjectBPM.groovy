
/**
 * Deploy IS projects
 */
class DeployProjectBPM {
	String projectName
	String candidate
	String jobDir
	
	def run() {
		Common.runCommand([Common.UCD_PATH + '/bin/deploy-BPM.sh', projectName, candidate, jobDir])
		File reportFile = new File("$jobDir/deployReport_BPM.xml")
		Common.verifyDeployReport(reportFile)
	}
	
	public static void main(String[] args) {
		if (args.length != 3) {
			println "groovy DeployProjectBPM projectName candidate jobDir"
			System.exit(1)
		}
		
		DeployProjectBPM deployer = new DeployProjectBPM(projectName: args[0], candidate: args[1], jobDir: args[2])
		deployer.run()
	}
}
