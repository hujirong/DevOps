package com.devops.urbancode.deploy

def client = new DeployClient(new File(System.getProperty('user.home') + '/devops/conf/devops.properties'))

def component = "core.udeploy-demo"
def version = "2.0"
def repo = "cppib-core"
def groupId = "com.cppib.core"
def artifactId = "udeploy-demo"

try {
	client.login()
	versionId = client.createVersion(component, version)
	println versionId
	
	client.createComponentVersionPropDefs(component, ['repo', 'groupId', 'artifactId'])
	client.setComponentVersionProperties(component, version, ['repo': repo, 'groupId': groupId, 'artifactId': artifactId])
	
	// client.addVersionFiles("opsys.ds.ifts", "4.0", new File('src/test/groovy'))
} finally {
	client.logout()
}
