// build plugin zip file
//
if (args.length != 1) {
  println "Usage: groovysh build-plugin.groovy plugin_dir"
  System.exit(0)
}

baseDir = args[0]
println "Create zip file for ${baseDir} ..."

// read plugin.xml
def plugin = new XmlSlurper().parse(new File(baseDir, 'plugin.xml'))
def version = plugin.header.identifier.@version

def ant = new AntBuilder()

// update info.xml
println "Update release-version ${version} in file info.xml"
ant.replaceregexp(file: "$baseDir/info.xml",
	match: "<release-version>.*</release-version>",
	replace: "<release-version>${version}</release-version>",
	byline: true)

def destFile = baseDir + '-' + version.toString() + '.zip'
ant.zip(basedir: baseDir, destfile: destFile, excludes: 'bin,build,test,.classpath,.project')
