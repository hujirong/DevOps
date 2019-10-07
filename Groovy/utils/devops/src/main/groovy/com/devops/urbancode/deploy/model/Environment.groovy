package com.devops.urbancode.deploy.model

class Environment extends UrbancodeObject {
	static String ARTIFACTORY_URL_DEV = 'http://artifactory-dev:8118/artifactory'
	static String ARTIFACTORY_URL_QA = 'http://artifactory-qa:8118/artifactory'
	static String ARTIFACTORY_URL_UAT = 'http://artifactory-uat:8118/artifactory'
	static String ARTIFACTORY_URL_PROD = 'http://artifactory-prod:8118/artifactory'
	
	private String type = UNDEFINE
	
	String artifactoryUrl
	
	// environment properties
	List props = []
	
	// component properties
	Map compProps = new TreeMap()
	
	// resource properties
	List resources = []
	  
	List getComponentProperties(String compName) {
		List result = null
		
		props.find {
			if (it.name == name) {
				result = it
				return true
			}
		}
		
		return result
	}
	
	List addComponentProperties(String compName) {
		List props = getComponentProperties(compName)
		if (props == null) {
			props = []
			compProps.put(compName, props)
		}
		
		return props
	}
	
	Resource getResource(String path) {
		Resource result = null
		
		resources.find {
			if (it.path == path) {
				result = it
				return true
			}
		}
		
		return result
	}
	
	void makeType(String uctype) {
		if (uctype == 'DEV Environment') {
			type = 'DEV'
		}
		
		else if (uctype == 'QA Environment') {
			type = 'QA'
		}

		else if (uctype == 'UAT Environment') {
			type = 'UAT'
		}
		else if (uctype == 'PROD Environment') {
			type = 'PROD'
		}
	}
	
	void setType(String type) {
		if (! (type in types)) {
			throw new Exception("Unsupported type $type, type must be one of ${types}")
		}
		
		this.type = type
	}

	String getType() {
		return type
	}
	
	String getUcdType() {
		def val = null
		
		if (type == 'DEV') {
			val = 'DEV Environment'
		}
		
		else if (type == 'QA') {
			val = 'QA Environment'
		}

		else if (type == 'UAT') {
			val = 'UAT Environment'
		}
		else if (type == 'PROD') {
			val = 'PROD Environment'
		}
		
		return val
	}
	
	String getDefaultArtifactoryUrl() {
		if (type == 'PROD') {
			return ARTIFACTORY_URL_PROD
		} else if (type == "UAT") {
			return ARTIFACTORY_URL_UAT
		} else if (type == "QA") {
			return ARTIFACTORY_URL_QA
		} else if (type == "DEV") {
			return ARTIFACTORY_URL_DEV
		}
		
		return 'NA'
	}
	
	//---------------------------------------------
	// Read DSL
	//---------------------------------------------
	def setBaseResource(String val) {
		Resource res = new Resource(path: val)
		resources.add(res)
	}
	
	static String getAgentArtifactoryUrl(String agentName) {
		def url = null
		
		agentName = agentName.toLowerCase()
		
		if (agentName.startsWith('dvi')) {
			url = ARTIFACTORY_URL_DEV
			
		} else if (agentName.startsWith('qai')) {
			url = ARTIFACTORY_URL_QA
			
		} else if (agentName.startsWith('uti') || agentName.startsWith('iti')) {
			url = ARTIFACTORY_URL_UAT
			
		} else if (agentName.startsWith('pri') || agentName.startsWith('drs')
			|| agentName.startsWith('prb')) {
			url = ARTIFACTORY_URL_PROD
		}
	}
}
