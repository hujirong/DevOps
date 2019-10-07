package com.devops.urbancode.deploy.model

class Team extends UrbancodeObject {
	List<Process> processes = []
	
	List<Component> components = []
	
	List<ComponentTemplate> componentTemplates = []
	
	List<Application> applications = []
	
	List<Resource> resources = []

	Process getProcess(String name) {
		for (Process p : processes) {
			if (p.name == name) {
				return p
			}
		}
		
		return null
	}

	Component getComponent(String name) {
		for (Component c : components) {
			if (c.name == name) {
				return c
			}
		}
		
		return null
	}

	ComponentTemplate getComponentTemplate(String name) {
		for (ComponentTemplate ct : componentTemplates) {
			if (ct.name == name) {
				return ct
			}
		}
		
		return null
	}

	Application getApplication(String name) {
		for (Application a : applications) {
			if (a.name == name) {
				return a
			}
		}
		
		return null
	}
	
	def setAppNameOnResources() {
		applications.each { Application app ->
			app.envs.each { Environment env ->
				env.resources.each { Resource baseRes ->
					Resource res = findResource(baseRes.getPath())
					if (res != null) {
						res.appName = app.name
					}
				}
			}
		}
	}
	
	Resource findResource(String path) {
		List items = path.tokenize('/')
		
		Resource result = null
		
		resources.find {
			if (it.name == items[0]) {
				// traverse children
				result = matchResource(it, items[1 .. -1])
				return true
			}
		}
		
		return result
	}
	
	Resource matchResource(Resource res, List items) {
		Resource child = res.getResourceByName(items[0])
		if (child != null) {
			if (items.size() == 1) {
				return child
			}
			
			return matchResource(child, items[1 .. -1])
		}
		
		return null
	}
	
	def verifyResources() {
		resources.each { it ->
			verifyResource(it)
		}	
	}
	
	def verifyResource(Resource res) {
		if (res.category == Resource.CATEGORY_COMP) {
			boolean found = false
			
			components.each { Component comp ->
				if (res.name == comp.name) {
					found = true
					return true
				}
			}
			
			assert found : "Component \"${res.name}\" is defined in resource tree but not in team component list"
		}
		
		res.children.each {
			verifyResource(it)
		}
	}
	
	//-----------------------------------
	// Output team configuration
	//-----------------------------------
	void output(TeamBuilder builder) {
		builder.team([:], name) {
			
			builder.comments('---------------------------------------------')
			builder.comments('Components (support multiple)')
			builder.comments('---------------------------------------------')
			
			components.each { comp ->
				outputComponent(builder, comp)
			}
			
			builder.comments('---------------------------------------------')
			builder.comments('Applications')
			builder.comments('---------------------------------------------')
			
			applications.each { app ->
				outputApplication(builder, app)
			}
			
			builder.comments('---------------------------------------------')
			builder.comments('Resources')
			builder.comments('---------------------------------------------')
			resources.each {
				outputResource(builder, it, true)
				builder.println()
			}
		}
	}
	
	void outputComponent(TeamBuilder builder, Component comp) {
		def attrs = [repositoryName: comp.repositoryName, groupId: comp.groupId, artifactId: comp.artifactId, 
			template: comp.template, description: comp.description, defaultVersionType: comp.defaultVersionType]
		
		builder.component(attrs, comp.name)
		builder.println() 
	}
	
	void outputApplication(TeamBuilder builder, Application app) {
		builder.application([description: app.description], app.name) {
			
			builder.comments('Application Environments')
			builder.comments('You must define type (DEV, QA, UAT or PROD)')
			app.envs.each { env ->
				builder.environment([type: env.type, description: env.description], env.name) {
					env.resources.each {
						builder.baseResource(it.getPath())
					}
				}
				
				builder.println()
			}
			
			builder.comments('Application Components')
			app.comps.each {
				builder.component(it.name)
			}
		}
		
		builder.println()
	}
	
	void outputResource(TeamBuilder builder, Resource res, boolean isTop) {
		def attrs = [description: res.description]
		def resType = res.getDisplayType()
		if (resType != null) {
			attrs['type'] = res.type
		}

		if (res.impersonationUser != null) {
			attrs['impersonationUser'] = res.impersonationUser
		}
		
		if (isTop) {
			attrs['ITAMAppID'] = res.ITAMAppID
			attrs['ITAMTeamName'] = res.ITAMTeamName
		}
		
		def resCategory = res.getDisplayCategory()

		if (resCategory == Resource.CATEGORY_AGENT) {		
			attrs['secure'] = res.secure
			
			builder.agent(attrs, res.name) {
				res.children.each {
					outputResource(builder, it, false)
				}
			}
		} else if (resCategory == Resource.CATEGORY_COMP) {
			builder.component(res.name)
		} else {
			builder.resource(attrs, res.name) {
				res.children.each {
					outputResource(builder, it, false)
				}
			}
		}
	}
	
	
	//---------------------------------------------
	// Read team configuration
	//---------------------------------------------
	static Team loadTeam(String name, Closure cl) {
		Team team = new Team(name: name)
		cl.setDelegate(team)
		cl.setResolveStrategy(Closure.DELEGATE_ONLY)
		cl.run()
		
		team.setAppNameOnResources()
		team.verifyResources()
		return team
	}
	
	def component(String name, Closure cl) {
		Component comp = new Component(name: name)
		cl.setDelegate(comp)
		cl.setResolveStrategy(Closure.DELEGATE_ONLY)
		cl.run()
		
		this.components.add(comp)
	}

	def application(String name, Closure cl) {
		Application app = new Application(name: name)
		cl.setDelegate(app)
		cl.setResolveStrategy(Closure.DELEGATE_ONLY)
		cl.run()
		
		this.applications.add(app)
	}
	
	def resource(String name, Closure cl) {
		Resource res = new Resource(name: name)
		cl.setDelegate(res)
		cl.setResolveStrategy(Closure.DELEGATE_ONLY)
		cl.run()

		this.resources.add(res)
	}
}
