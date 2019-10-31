
/**
 * Deploy IS projects
 */
class DeployProjectIS {
	String projectName
	String candidate
	String jobDir
	
	def run() {
		Common.runCommand([Common.UCD_PATH + '/bin/deploy-IS.sh', projectName, candidate, jobDir])
		File reportFile = new File("$jobDir/deployReport_IS.xml")
		Common.verifyDeployReport(reportFile)
	}
	
	public static void main(String[] args) {
		if (args.length != 3) {
			println "groovy DeployProjectIS projectName candidate jobDir"
			System.exit(1)
		}
		
		DeployProjectIS deployer = new DeployProjectIS(projectName: args[0], candidate: args[1], jobDir: args[2])
		deployer.run()
	}
}
