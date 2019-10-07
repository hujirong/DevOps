package com.devops.urbancode.admin

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.Method

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.devops.urbancode.deploy.model.Environment
import com.devops.urbancode.deploy.model.Property
import com.devops.urbancode.deploy.model.Resource

@Slf4j
@Parameters(commandDescription = "Update environment properties")
class EnvPropsUpdate extends UdeployCmd {
	@Parameter(names = "-F", description = "Input file", required = true)
	String fileName

	String appName
	String appId
	
	String envName
	
	// include resource properties
	@Parameter(names = "-RP", description = "Include resource properties", required = false, arity = 1)
	boolean includeResources = false
	
	Environment oldEnv
	
	Environment newEnv
	
	@Override
	public void run() {
		// load environments
		loadNewEnv()
		loadOldEnv()
		
		verifyAppEnv()
		updateEnv()
	}
	
	void loadNewEnv() {
		log.info("Loading new values from ${fileName}")
		def appXml = new XmlParser().parse(new File(fileName))
		
		// verify XML
		verifyXml(appXml)
		
		appName = appXml.'@name'
		
		log.info("Create environment from XML")
		def envXml = appXml.Environments.Environment[0]
		newEnv = createEnvFromXml(envXml)
		
		envName = newEnv.name
	}

	void loadOldEnv() {
		log.info("Read environment properties from UrbanCode Deploy server ...")
		EnvPropsDump dumpCmd = new EnvPropsDump(user: user, password: password, appName: appName, envName: envName)
		dumpCmd.httpBuilder = httpBuilder
		dumpCmd.init()
		dumpCmd.query()
		
		// make sure old environment exists
		if (dumpCmd.app.envs.size() != 1) {
			throw new Exception("Please create environment ${envName} first and then use this program.")
		}
		
		appId = dumpCmd.app.id
		oldEnv = dumpCmd.app.envs[0]
	}
	
	void verifyXml(appXml) {
		assert appXml.'@name' != null : "Need Application name"
		assert appXml.Environments != null : "Need Environments in Application"
		assert appXml.Environments.Environment.size() == 1 : "Only one Environment is allowed in Environments"
		assert appXml.Environments.Environment[0].'@name' != null : "Need Environment name"
	} 
	
	// verify application/environment
	void verifyAppEnv() {
		// verify components
		newEnv.compProps.keySet().each { compName ->
			assert oldEnv.compProps.get(compName) != null : "Component ${compName} is not exist"
		}
		
		// verify resources
		newEnv.resources.each {
			def oldr = oldEnv.getResource(it.path)
			verifyResource(oldr, it)
		}
	}
	
	void verifyResource(Resource oldResource, Resource newResource) {
		assert oldResource != null : "Cannot find resource path: ${newResource.path}"
		
		newResource.children.each {
			def oldr = oldResource.getResource(it.path)
			verifyResource(oldr, it)
		}
	}
	
	Environment createEnvFromXml(envXml) {
		Environment env = new Environment(name: envXml.'@name')
		
		// add environment Properties
		envXml.Properties.Property.each {
			Property prop = newProp(it)
			env.props.add(prop)
		}
		
		// add component properties
		envXml.ComponentEnvironmentProperties.Component.each { compXml ->
			List props = env.addComponentProperties(compXml.'@name')
				
			compXml.Properties.Property.each {
				Property prop = newProp(it)
				props.add(prop)
			}
		}
		
		envXml.BaseResources.Resource.each {
			Resource rsc = createResource(it)
			env.resources.add(rsc)
		}
				
		return env
	}
	
	Resource createResource(xml) {
		Resource rsc = new Resource(name: xml.'@name', path: xml.'@path')
		
		// properties
		xml.Properties.Property.each {
			Property prop = newProp(it)
			rsc.props.add(prop)
		}
		
		// child resources
		xml.Resource.each { childXml ->
			Resource child = createResource(childXml)
			rsc.children.add(child)
		}
				
		return rsc
	}
	
	Property newProp(propXml) {
		Property prop = new Property(name: propXml.'@name',
			value: propXml.'@value',
			secure: propXml.'@secure'.toBoolean(),
			description: propXml.'@description')
		
		return prop
	}
	
	Property getProp(List props, String name) {
		for (Property p : props) {
			if (p.name == name) {
				return p
			}
		}
	}
	
	boolean needUpdate(Property oldProp, Property newProp) {
		if (oldProp.name == newProp.name) {
			if (newProp.secure) {
				// don't update secure property
				return false
			}
			
			if (oldProp.value != newProp.value) {
				return true
			}
			
			/*
			if (oldProp.description != newProp.description) {
				return true
			}
			*/
		}
		
		return false
	}
	
	void updateEnv() {
		// set environment properties
		log.info("")
		newEnv.props.each { newProp ->
			setEnvProp(newProp)
		}
		
		// set environment component properties
		log.info("")
		newEnv.compProps.each { comp, props ->
			props.each {
				setEnvCompProp(comp, it)
			}	
		}
		
		// set resource properties
		if (includeResources) {
			log.info("")
			
			newEnv.resources.each { newr ->
				def oldr = oldEnv.getResource(newr.path)
				setResourceProperties(oldr, newr)
			}
		}
	}
	
	void setEnvProp(Property newProp) {
		def changed = false
		
		Property oldProp = getProp(oldEnv.props, newProp.name)
		
		if (oldProp == null) {
			changed = true
		} else if (needUpdate(oldProp, newProp)) {
			changed = true
		}

		if (!changed) {
			return
		}
		
		log.info("Set environment property: env=${envName}, name=${newProp.name}, value=${newProp.value}")

		def result = httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + "/environment/propValue"
			uri.query = [ application: appName, environment: envName, name: newProp.name, value: newProp.value, isSecure: newProp.secure ]
			//requestContentType: ContentType.JSON
				
			response.failure = { resp, reader ->
				log.error("Update ${prop.name} failed")
				log.error(reader.text)
				throw new Exception("Update property failed")
			}
		}
	}
	
	void __updateEnvProp(String appId, String envId, String propId, Property prop) {
		log.info("Update: app=${appName}, env=${envName}, name=${prop.name}, value=${prop.value}")
		
		def query = "applications%26${appId}%26environments%26${envId}%26propSheet.-1/propValues/${prop.name}"

		def result = httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
			uri.path = webUrl + PROP_API + query
			 
			body = [ name: prop.name, value: prop.value, secure: prop.secure, 
				description: prop.description, existingId: propId ]
			 
			response.failure = { resp, reader ->
				log.error("Update ${prop.name} failed")
				log.error(reader.text)
				throw new Exception("Update property failed")
			}
		 }
	 }
 
	 void __createEnvProp(String appId, String envId, Property prop) {
		def query = "applications&${appId}&environments&${envId}&propSheet.-1"
		query = URLEncoder.encode(query, "UTF-8") + "/propValues"
 
		def result = httpBuilder.request(webUrl + PROP_API + query, Method.PUT, ContentType.JSON) { req ->
		 body = [ name: prop.name, value: prop.value, secure: prop.secure, description: prop.description ]
			 
			 response.failure = { resp, reader ->
				 log.error("Create ${prop.name} failed")
				 log.error(reader.text)
				 throw new Exception("Create property failed")
			 }
		 }
	 }
	 
	void setEnvCompProp(String comp, Property newProp) {
		def changed = false
		
		List oldProps = oldEnv.compProps[comp]
		Property oldProp = getProp(oldProps, newProp.name)
		
		if (oldProp == null) {
//			throw new Exception("Cannot find component environment property: component=${comp}, name=${newProp.name}")
			changed = true
		} else if (needUpdate(oldProp, newProp)) {
			changed = true
		}

		if (!changed) {
			return
		}
		
		log.info("Set component environment property: env=${envName}, component=${comp}, name=${newProp.name}, value=${newProp.value}")
			
		def result = httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + "/environment/componentProperties"
			uri.query = [ application: appName, environment: envName, component: comp,
				name: newProp.name, value: newProp.value, isSecure: newProp.secure ]
				
			response.failure = { resp, reader ->
				log.error("Update ${prop.name} failed")
				log.error(reader.text)
				throw new Exception("Update property failed")
			}
		}
	}
		
	def setResourceProperties(Resource oldr, Resource newr) {
		newr.props.each { newProp ->
			setResorceProp(oldr, newProp)
		}
		
		// child resources
		newr.children.each {
			def oldrc = oldr.getResource(it.path)
			setResourceProperties(oldrc, it)
		}
	}

	void setResorceProp(Resource oldr, Property newProp) {
		def changed = false
		
		Property oldProp = getProp(oldr.props, newProp.name)
		
		if (oldProp == null) {
			changed = true
		} else if (needUpdate(oldProp, newProp)) {
			changed = true
		}

		if (!changed) {
			return
		}
		
		log.info("Set resource property: path=${oldr.path}, name=${newProp.name}, value=${newProp.value}")
			
		def result = httpBuilder.request(Method.PUT, ContentType.JSON) { req ->
			uri.path = webUrl + PUBLIC_API + "/resource/setProperty"
			uri.query = [ resource: oldr.path,
				name: newProp.name, value: newProp.value, isSecure: newProp.secure ]
				
			response.failure = { resp, reader ->
				log.error("Update ${prop.name} failed")
				log.error(reader.text)
				throw new Exception("Update property failed")
			}
		}
	}
}
