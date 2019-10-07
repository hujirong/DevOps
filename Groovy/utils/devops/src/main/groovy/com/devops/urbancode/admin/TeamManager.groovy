package com.devops.urbancode.admin

import groovy.util.logging.Slf4j

import org.codehaus.groovy.runtime.StackTraceUtils

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException

@Slf4j
class TeamManager {
	void run(cmd) {
		cmd.init()
		cmd.process()
	}
	
	public static void main(String[] args) {
		TeamManager teamMgr = new TeamManager()

		JCommander jcmd = new JCommander()
		
		TeamTemplate templateCmd = new TeamTemplate()
		jcmd.addCommand("template", templateCmd)

		TeamImport importCmd = new TeamImport()
		jcmd.addCommand("import", importCmd)
		
		TeamExport exportCmd = new TeamExport()
		jcmd.addCommand("export", exportCmd)
		
		GroupManager groupCmd = new GroupManager()
		jcmd.addCommand("group", groupCmd)
		
		def cmd = null
		
		try {
			jcmd.parse(args)
			
			String strCmd = jcmd.getParsedCommand()
			if (strCmd == null) {
				jcmd.usage()
				System.exit(0)
			}
			
			if (strCmd.equals("export")) {
				cmd = exportCmd
			} else if (strCmd.equals("import")) {
				cmd = importCmd
			} else if (strCmd.equals("template")) {
				cmd = templateCmd
			} else if (strCmd.equals("group")) {
				cmd = groupCmd
			} else {
				jcmd.usage()
				System.exit(0)
			}			
			
			teamMgr.run(cmd)
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage())
			jcmd.usage()
			System.exit(-1)
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("TeamManager FAILED", ta)
			System.exit(-2)
		}
		
		System.exit(0)
	}
}
