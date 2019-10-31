/**
 * Deploy CAF projects
 * 
 * This script runs multiple CAF projects, for each of them:
 *   project-automation
 *   deploy-project
 *   
 * @author fhou
 *
 */
class DeployProjectsCAF {
	Properties props = new Properties()
	
	DeployProjectsCAF(String inPropsFile, String outPropsFile) {
		println "Load properties from file $inPropsFile"
		new File(inPropsFile).withInputStream {
			props.load(it)
		}
	}
	
	def deployProject(String project, String candidate) {
		// project automator
		def projectFile = props.JOB_DIR + '/' + project
		
		println "====>  Project automator for CAF $projectFile, candidate is $candidate"
		Common.runCommand([Common.UCD_PATH + '/bin/project-automator.sh', projectFile])
		
		if (props.STAGE == 'ALL' || props.STAGE == 'FULL' || props.STAGE == 'DEPLOY') {
			def projectName = props.PROJECT_CAF
			println "====>  Deploy CAF project $projectName, candidate is $candidate"
			
			Common.runCommand([Common.UCD_PATH + '/bin/deploy-CAF.sh', projectName, candidate, props.JOB_DIR, project])
			File reportFile = new File("${props.JOB_DIR}/deployReport_CAF_${project}.xml")
			Common.verifyDeployReport(reportFile)
		}
	}
	
	def run() {
		def cafCandidate = props.CAF_CANDIDATE
		
		def projects = props.CAF_PROJECTS.split(',')
		projects.each { project ->
			deployProject(project, cafCandidate)
		}
	}
	
	public static void main(String[] args) {
		if (args.length != 2) {
			println "groovy DeployProjectsCAF inPropsFile outPropsFile"
			System.exit(1)
		}
		
		DeployProjectsCAF deployer = new DeployProjectsCAF(args[0], args[1])
		deployer.run()
	}
}
