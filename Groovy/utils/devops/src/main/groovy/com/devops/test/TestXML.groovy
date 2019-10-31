package com.devops.test

//def environmentName = 'DEV'
def environmentName = 'QA'
//def environmentName = 'UAT'
//def environmentName = 'PROD'

File controlFile = new File('db-deploy.xml')
println "Load control file " + controlFile.getCanonicalPath()

if (!controlFile.exists()) {
  throw new Exception("File is not exist: " + controlFile.getCanonicalPath())
}

// read control xml file
def dbdeploy = new XmlSlurper().parse(controlFile)
def env = dbdeploy.env.find { it['@name'] == environmentName }

if (env.size() == 0) {
  throw new Exception("Cannot find environment environmentName in controlFile")
}

// load files
def sqlFiles = ''
for (f in env.file.list()) {
  def sqlf = new File(f.text())
  if (!sqlf.exists()) {
	throw new Exception("File is not exist: " + sqlf.getCanonicalPath())
  }

  sqlFiles = sqlFiles + f.text() + '\n'
}

println "Run SQL files ..."
println "$sqlFiles"

