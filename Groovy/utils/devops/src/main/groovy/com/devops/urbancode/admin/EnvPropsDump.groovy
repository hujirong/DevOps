package com.devops.urbancode.admin

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.devops.urbancode.deploy.model.Application
import com.devops.urbancode.deploy.model.Environment
import com.devops.urbancode.deploy.model.Property
import com.devops.urbancode.deploy.model.Resource

@Slf4j
@Parameters(commandDescription = "Dump environment properties")
class EnvPropsDump extends UdeployCmd {
	@Parameter(names = "-APP", description = "Application name", required = true)
	String appName

	@Parameter(names = "-ENV", description = "Enironment name", required = false)
	String envName

	@Parameter(names = "-F", description = "Output file", required = false)
	String fileName

	Application app
	
	List components = []
	
	@Override
	public void run() {
		query()
		output()
	}
	
	void query() {
		app = new Application(name: appName)
		
		// get app ID
		def appJson = httpBuilder.get(path: PUBLIC_API + '/application/info',
			query: [application: appName])

		app.id = appJson.id
		
		// get all components
		def compList = httpBuilder.get(path: PUBLIC_API + '/application/componentsInApplication',
			query: [application: appName])

		compList.each {
			components.add(it.name)
		}

		queryEnvs(envName)
	}
	
	void output() {
		Writer writer = null
		
		if (fileName?.trim()) {
			writer = new FileWriter(new File(fileName))
		} else {
			writer = new OutputStreamWriter(System.out)
		}
	
		MarkupBuilder xml = new MarkupBuilder(writer)
		
		xml.Application(name: app.name) {
			Environments {
				app.envs.each { env ->
					outputEnvironment(xml, env)
				}
			}
		}
	}
	
	def outputEnvironment(builder, Environment env) {
		builder.Environment(name: env.name) {
			Properties {
				env.props.each { prop ->
					outputProperty(builder, prop)
				}
			}
			
			ComponentEnvironmentProperties {
				env.compProps.each { comp, props ->
					Component(name: comp) {
						Properties {
							props.each { prop ->
								outputProperty(builder, prop)
							}
						}
					}
				}
			}
			
			BaseResources {
				env.resources.each {
					outputResource(builder, it)
				}
			}
		}
	}
	
	void outputResource(builder, Resource resource) {
		builder.Resource(getResourceAttributes(resource)) {
			Properties {
				resource.props.each { prop ->
					outputProperty(builder, prop)
				}
			}

			// child resources
			resource.children.each {
				outputResource(builder, it)
			}
		}
	}
	
	def getResourceAttributes(Resource resource) {
		def attrs = [name: resource.name, path: resource.path]
		
		if (resource.impersonationUser?.trim()) {
			attrs['impersonationUser'] = resource.impersonationUser
			attrs['impersonationUseSudo'] = resource.impersonationUseSudo
			attrs['impersonationForce'] = resource.impersonationForce
		}
		
		return attrs
	}
	
	void outputProperty(builder, Property prop) {
		def val = prop.value
		
		if (prop.secure) {
			val = "crypt[" + System.currentTimeMillis() + "]"
		}
		
		builder.Property(name: prop.name, value: val, secure: prop.secure, description: prop.description)
	}
	
	void queryEnvs(envName) {
		def envList = httpBuilder.get(path: PUBLIC_API + '/application/environmentsInApplication',
			query: [application: appName])

		
		envList.each {
			if (envName?.trim()) {
				// only query specific environment
				if (envName == it.name) {
					queryEnvProps(it.name, it.id)
				}
			} else {
				// query all environments
				queryEnvProps(it.name, it.id)
			}
		}
	}
	
	void queryEnvProps(String envName, String envId) {
		// query environment properties
		def env = new Environment(name: envName, id: envId)
		app.envs.add(env)
		
		def propList = httpBuilder.get(path: PUBLIC_API + '/environment/getProperties',
			query: [application: appName, environment: envName])

		propList.each {
			def prop = new Property(id: it.id, name: it.name, value: it.value, description: it.description, secure: it.secure)
			env.props.add(prop)
		}
		
		// query component environment properties
		components.each {
			queryComponentEnvProps(env, it)
		}
		
		// query resource properties
		def baseResourceList = httpBuilder.get(path: PUBLIC_API + '/environment/getBaseResources',
			query: [application: appName, environment: envName])

		baseResourceList.each {
			Resource resource = new Resource(name: it.name, id: it.id, path: it.path)
			env.resources.add(resource)
			queryResourceProperties(resource)
		}
	}
	
	void queryComponentEnvProps(Environment env, String compName) {
		List compEnvProps = env.addComponentProperties(compName)
		
		def propList = httpBuilder.get(path: PUBLIC_API + '/environment/componentProperties',
			query: [application: appName, environment: env.name, component: compName])
		
		
		propList.each {
			def prop = new Property(id: it.id, name: it.name, value: it.value, description: it.description, secure: it.secure)
			compEnvProps.add(prop)
		}
	}
	
	void queryResourceProperties(Resource rsc) {
		// Resource Basic Settings
		def info = httpBuilder.get(path: PUBLIC_API + '/resource/info',
			query: [resource: rsc.id])

		if (info.impersonationUser?.trim()) {
			rsc.impersonationUser = info.impersonationUser
			rsc.impersonationUseSudo = info.impersonationUseSudo
			rsc.impersonationForce = info.impersonationForce
		}
		
		// Resource Properties
		def propList = httpBuilder.get(path: PUBLIC_API + '/resource/getProperties',
			query: [resource: rsc.id])
		
		
		propList.each {
			def prop = new Property(id: it.id, name: it.name, value: it.value, description: it.description, secure: it.secure)
			rsc.props.add(prop)
		}

		// query child resources
		def childList = httpBuilder.get(path: PUBLIC_API + '/resource',
			query: [parent: rsc.id])
		
		
		childList.each {
			Resource child = new Resource(name: it.name, id: it.id, path: it.path)
			rsc.children.add(child)
			queryResourceProperties(child)
		}
	}
}
