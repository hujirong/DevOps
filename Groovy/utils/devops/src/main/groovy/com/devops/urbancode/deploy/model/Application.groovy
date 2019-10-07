package com.devops.urbancode.deploy.model

class Application extends UrbancodeObject {
	// environments
	List envs = []
	
	// components
	List comps = []	
	
	def setComponent(String name) {
		Component comp = new Component(name: name)
		comps.add(comp)
	}
	
	def environment(String name, Closure cl) {
		Environment env = new Environment(name: name)
		cl.setDelegate(env)
		cl.setResolveStrategy(Closure.DELEGATE_ONLY)
		cl.run()

		envs.add(env)
	}
	
	Component getComponent(String name) {
		Component comp = null
		
		comps.find {
			if (it.name == name) {
				comp = it
				return true
			}
		}
		
		return comp
	}
	
	Environment getEnvironment(String name) {
		Environment env = null
		
		envs.find {
			if (it.name == name) {
				env = it
				return true
			}
		}
		
		return env
	}
}
