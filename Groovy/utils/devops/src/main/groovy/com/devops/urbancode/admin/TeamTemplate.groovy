package com.devops.urbancode.admin

import groovy.util.logging.Slf4j
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.devops.urbancode.deploy.model.Application
import com.devops.urbancode.deploy.model.Component
import com.devops.urbancode.deploy.model.Environment
import com.devops.urbancode.deploy.model.Resource
import com.devops.urbancode.deploy.model.Team
import com.devops.urbancode.deploy.model.TeamBuilder

/* Create Team teamplate file 
 */
@Slf4j
@Parameters(commandDescription = "Create team resource template")
class TeamTemplate {
	static String header = 'This is team resource definition file'
	
	@Parameter(names = "-F", description = "Output file", required = true)
	String fname

	Team team
	
	void init() {}
	
	void process() {
		buildTeam()
		output()
	}
	
	void buildTeam() {
		def teamName = 'TeamDemo'
		log.info("Build team ${teamName}")
		
		team = new Team(name: teamName)
		
		def compName = 'core-comp-demo'
		def appName = 'core-demo100'
		
		Component comp = new Component(name: compName, 
			description: 'This is CoreTech component demo',
			template: 'OracleTemplate',
			repositoryName: 'cppib-core',
			groupId: 'com/cppib/core',
			artifactId: 'core-comp-demo')
			
		team.components.add(comp)
		
		// build resources
		Resource res = new Resource(name: appName)
		team.resources.add(res)
		
		Resource devRes = new Resource(name: 'DEV',	type: 'DEV')
		res.addChild(devRes)
		
		Resource devAgentRes = new Resource(name: 'dviappvmcore99.cppib.ca',
			category: Resource.CATEGORY_AGENT,
			secure: true,
			impersonationUser: 'svc_dev_demo99')
		
		devRes.addChild(devAgentRes)
		
		Resource devCompRes = new Resource(name: compName,
			category: Resource.CATEGORY_COMP)
		devAgentRes.addChild(devCompRes)
		
		Resource qaRes = new Resource(name: 'QA',	type: 'QA')
		res.addChild(qaRes)
		
		Resource qaAgentRes = new Resource(name: 'qaiappvmcore88.cppib.ca',
			category: Resource.CATEGORY_AGENT,
			secure: true,
			impersonationUser: 'svc_demo_qa88')
		qaRes.addChild(qaAgentRes)
		
		Resource qaCompRes = new Resource(name: compName,
			category: Resource.CATEGORY_COMP)
		qaAgentRes.addChild(qaCompRes)
		
		// application
		Application app = new Application(name: appName)
		app.comps.add(comp)
		team.applications.add(app)
		
		Environment devEnv = new Environment(name: 'DEV', type: 'DEV')
		app.envs.add(devEnv)
		devEnv.resources.add(new Resource(path: "/${appName}/DEV"))
		
		Environment qaEnv = new Environment(name: 'QA', type: 'QA')
		app.envs.add(qaEnv)
		qaEnv.resources.add(new Resource(path: "/${appName}/QA"))
	}
	
	void output() {
		log.info("Output team resources to file $fname")
		
		FileWriter fw = new FileWriter(new File(fname))

		TeamBuilder builder = new TeamBuilder(fw)
		builder.comments(header)
		team.output(builder)
		
		fw.close()
	}
}
