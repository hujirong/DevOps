package com.devops.urbancode.admin

import groovy.util.logging.Slf4j;

import org.codehaus.groovy.runtime.StackTraceUtils

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException

/**
 * Manage environment properties 
 *
 */
@Slf4j
class EnvPropsManager {
	void run(UdeployCmd cmd) {
		cmd.init()
		cmd.process()
	}
	
	public static void main(String[] args) {
		EnvPropsManager envPropsMgr = new EnvPropsManager()

		JCommander jcmd = new JCommander();
		
		EnvPropsUpdate updateCmd = new EnvPropsUpdate();
		jcmd.addCommand("update", updateCmd);
		
		EnvPropsDump dumpCmd = new EnvPropsDump();
		jcmd.addCommand("dump", dumpCmd);
		
		UdeployCmd cmd = null
		
		try {
			jcmd.parse(args)
			
			String strCmd = jcmd.getParsedCommand()
			if (strCmd == null) {
				jcmd.usage()
				System.exit(0)
			}
			
			if (strCmd.equals("dump")) {
				cmd = dumpCmd
			} else if (strCmd.equals("update")) {
				cmd = updateCmd
			} else {
				jcmd.usage();
				System.exit(0);
			}			
			
			envPropsMgr.run(cmd)
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage());
			jcmd.usage();
			System.exit(-1);
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("EnvPropsManager FAILED", ta)
			System.exit(-2);
		}
		
		System.exit(0);
	}
}
