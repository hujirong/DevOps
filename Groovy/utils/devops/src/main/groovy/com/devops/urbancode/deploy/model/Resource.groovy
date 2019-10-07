package com.devops.urbancode.deploy.model

class Resource extends UrbancodeObject {
	static String CATEGORY_AGENT = 'agent'
	static String CATEGORY_COMP = 'component'
	
	String path
	List children = []
	List props = []
	
	Resource parent
	
	// urbancode properties
	String impersonationUser
	String impersonationUseSudo
	String impersonationForce

	// top level resource (application) only
	String ITAMAppID
	String ITAMTeamName
	
	private String category
	private String type

	// secure Tag
	boolean secure
	
	// environment resource
	boolean isEnvironmentResource = false
	
	// only apply to environmentResource
	String appName
	
	boolean inheritTeam = false
	
	def addChild(Resource res) {
		children.add(res)
		res.parent = this
		
		if (res.category == CATEGORY_AGENT) {
			// assume agent resource always under environment resource
			isEnvironmentResource = true 
		}
	}
	
	// search child resource by path string
	Resource getResource(String path) {
		Resource result = null
		
		children.find {
			if (it.path == path) {
				result = it
				return true
			}
		}
		
		return result
	}
	
	// get resource by name
	Resource getResourceByName(String name) {
		Resource result = null
		
		children.find {
			if (it.name == name) {
				result = it
				return true
			}
		}
		
		return result
	}
	
	void makeCategory(ucRes) {
		category = null
		
		if (ucRes.type == 'agent') {
			category = CATEGORY_AGENT
		} else if (ucRes.type == 'subresource' && ucRes.role && ucRes.role.specialType == 'COMPONENT') {
			category = CATEGORY_COMP
		}
	}
	
	String getDisplayCategory() {
		if (category == CATEGORY_AGENT) {
			return CATEGORY_AGENT
		} else if (category == CATEGORY_COMP) {
			return CATEGORY_COMP
		}
		
		return "resource"
	}
	
	void makeType(String uctype) {
		type = null
		
		if (uctype == 'DEV Resource' || uctype == 'DEV Component Resource' || uctype == 'DEV Environment Resource') {
			type = 'DEV'
		}
		
		else if (uctype == 'QA Resource' || uctype == 'QA Component Resource' || uctype == 'QA Environment Resource') {
			type = 'QA'
		}

		else if (uctype == 'UAT Resource' || uctype == 'UAT Component Resource' || uctype == 'UAT Environment Resource') {
			type = 'UAT'
		}
		else if (uctype == 'PROD Resource' || uctype == 'PROD Component Resource' || uctype == 'PROD Environment Resource') {
			type = 'PROD'
		}
	}
	
	void setType(String type) {
		if (! (type in types)) {
			throw new Exception("Unsupported type $type, type must be one of ${types}")
		}
		
		this.type = type
	}
	
	String getDisplayType() {
		if (parent == null) {
			return type
		}
		
		def parentType = parent.getDisplayType()
		if (type == parentType) {
			return null
		}
		
		return type
	}
	
	String getType() {
		if (type?.trim()) {
			return type
		}
		
		if (parent != null) {
			return parent.getType()
		}
		
		return null
	}
	
	String getUcdType() {
		def val = getType()
		def ucdType = null
		
		if (val == 'DEV') {
			ucdType = 'DEV Resource'
			
			if (isEnvironmentResource) {
				ucdType = 'DEV Environment Resource'
			}
			
			if (category == CATEGORY_COMP) {
				ucdType = 'DEV Component Resource'
			}
		}
		
		else if (val == 'QA') {
			ucdType = 'QA Resource'
			
			if (isEnvironmentResource) {
				ucdType = 'QA Environment Resource'
			}
			
			if (category == CATEGORY_COMP) {
				ucdType = 'QA Component Resource'
			}
		}

		else if (val == 'UAT') {
			ucdType = 'UAT Resource'
			
			if (isEnvironmentResource) {
				ucdType = 'UAT Environment Resource'
			}
			
			if (category == CATEGORY_COMP) {
				ucdType = 'UAT Component Resource'
			}
		}
		
		else if (val == 'PROD') {
			ucdType = 'PROD Resource'
			
			if (isEnvironmentResource) {
				ucdType = 'PROD Environment Resource'
			}
			
			if (category == CATEGORY_COMP) {
				ucdType = 'PROD Component Resource'
			}
		}
		
		return ucdType
	}
	
	String getPath() {
		if (path != null) {
			return path
		}
		
		def li = []
		
		Resource r0 = this
		while (r0 != null) {
			li.add(r0.name)
			r0 = r0.parent
		}
	
		li = li.reverse()
		path = '/' + li.join('/')
		return path
	}
	
	String getApplication() {
		if (category == CATEGORY_AGENT) {
			return parent.appName
		}
		
		return ''
	}
	
	//---------------------------------------------
	// Read DSL
	//---------------------------------------------
	def resource(String name, Closure cl) {
		Resource res = new Resource(name: name)
		cl.setDelegate(res)
		cl.setResolveStrategy(Closure.DELEGATE_ONLY)
		cl.run()

		addChild(res)
	}

	def agent(String name, Closure cl) {
		Resource res = new Resource(name: name, category: CATEGORY_AGENT)
		res.secure = false
		cl.setDelegate(res)
		cl.setResolveStrategy(Closure.DELEGATE_ONLY)
		cl.run()

		addChild(res)
	}

	def setComponent(String name) {
		Resource res = new Resource(name: name, category: CATEGORY_COMP)
		addChild(res)
	}
}
