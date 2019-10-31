/**
 * Build IS project file for wmdeployer
 * @author fhou
 *
 */

import java.util.Properties;

import groovy.xml.MarkupBuilder
import groovy.xml.MarkupBuilderHelper

class BuildProjectIS {
	boolean include_JDBC_polling_notifications = false

	Properties confProps = new Properties()

	// wmdeployer credentials
	Properties wmdeployProps = new Properties()

	String jobDir
	String projectName

	def loadConf() {
		File f = new File(jobDir, Common.WMDEPLOY_CONF_FILE)
		if (!f.exists()) {
			println "File ${f.getpath()} is not exist"
			System.exit(1)
		}

		f.withInputStream { is ->
			confProps.load(is)
		}

		// load wmdeployProps
		new File("${Common.UCD_PATH}/bin/deploymentCredentials.cnf").withInputStream { stream ->
			wmdeployProps.load(stream)
		}
	}

	def writeProjectFile() {
		File projectFile = new File(jobDir, Common.IS_PROJECT_FILE)
		println "Write IS project file ${projectFile.getPath()}"

		projectName = confProps.PROJECT_NAME

		Writer writer = new FileWriter(projectFile)
		MarkupBuilder xmlBuilder = new MarkupBuilder(writer)
		
		def helper = new MarkupBuilderHelper(xmlBuilder)
		helper.xmlDeclaration([version:'1.0', encoding:'UTF-8', standalone:'no'])

		xmlBuilder.DeployerSpec(exitOnError: 'false', sourceType: 'Repository') {
			DeployerServer {
				host(wmdeployProps.host + ':' + wmdeployProps.port)
				user(wmdeployProps.user)
				pwd(wmdeployProps.pwd)
			}

			Environment {
				Repository {
					repalias(name: "Repo.$projectName") {
						type('FlatFile')
						urlOrDirectory(confProps.ABE_REPO_DIR)
						Test('true')
					}
				}
			}

			Projects {
				Project(description: "$projectName IS Deployment", ignoreMissingDependencies: true,
					name: confProps.PROJECT_IS, overwrite: true, type: 'Repository')
				{
					DeploymentSet(autoResolve: 'ignore',
					description: "$projectName DeploymentSet", name: "Set.${projectName}",
					srcAlias: "Repo.$projectName") {
						writeComponents(xmlBuilder)
					}

					DeploymentMap(description: "$projectName DeploymentMap", name: "Map.$projectName")

					MapSetMapping(mapName: "Map.$projectName", setName: "Set.$projectName") {
						confProps.IS_target.split(',').each {
							alias(it, type: 'IS')
						}
					}

					DeploymentCandidate(description: "$projectName candidate",
					mapName: "Map.$projectName", name: "Candidate.$projectName")

					// full deployment
					DeploymentSet(autoResolve: 'ignore',
					description: "$projectName full DeploymentSet", name: "Set.full.$projectName",
					srcAlias: "Repo.$projectName")
					{ writeComposites(xmlBuilder) }

					DeploymentMap(description: "$projectName Full DeploymentMap", name: "Map.full.$projectName")

					MapSetMapping(mapName: "Map.full.$projectName", setName: "Set.full.$projectName") {
						confProps.IS_target.split(',').each {
							alias(it, type: 'IS')
						}
					}

					DeploymentCandidate(description: "$projectName full candidate",
					mapName: "Map.full.$projectName", name: "Candidate.full.$projectName")
				}
			}
		}

		writer.close()
	}

	def writeComponents(xmlBuilder) {
		confProps.IS_packages.split(',').each { writeRegularAssets_IS(xmlBuilder, it) }
	}

	// load IS assets (exclude adapters)
	def writeRegularAssets_IS(xmlBuilder, packageName) {
		def acdlFile = new File(confProps.ABE_REPO_DIR + '/IS/' + packageName + '.acdl')
		println "Load acdl file ${acdlFile.getPath()}"

		def acdlXml = new XmlParser().parse(acdlFile)

		acdlXml.asset.each { asset ->
			def type = asset.'implementation.generic'[0].'@type'

			// check if include this IS asset type
			def includeThis = true

			if (type == 'ispackage') {
				includeThis = false
			} else if (type == 'artconnection') {
				includeThis = false
			} else if (type == 'artlistenernotification') {
				includeThis = false
			} else if (type == 'artlistener') {
				includeThis = false
			} else if (type == 'artpollingnotification') {
				if (include_JDBC_polling_notifications) {
					includeThis = false
				} else {
					includeThis = false
				}
			}

			if (includeThis) {
				xmlBuilder.Component(componentType: type, compositeName: packageName, displayName: asset.'@displayName',
					name: asset.'@name', srcAlias: "Repo.$projectName", type: "IS")
			}
		}
	}

	def writeComposites(xmlBuilder) {
		confProps.IS_packages.split(',').each { packageName ->
			xmlBuilder.Composite(displayName: packageName, name: packageName, srcAlias: "Repo.$projectName", type: "IS")
		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			println "Usage: groovy BuildProjectIS.groovy JOB_DIR"
			System.exit(1)
		}

		BuildProjectIS isProject = new BuildProjectIS()
		isProject.jobDir = args[0]
		isProject.loadConf()
		isProject.writeProjectFile()
	}
}
