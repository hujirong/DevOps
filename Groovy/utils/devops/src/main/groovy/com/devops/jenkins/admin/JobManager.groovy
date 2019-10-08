package com.devops.jenkins.admin

import groovy.util.logging.Slf4j;
import org.codehaus.groovy.runtime.StackTraceUtils
import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException

/**
 * Manage environment properties 
 *
 */
@Slf4j
class JobManager {
	void run(JobRun cmd) {		
		cmd.run()
	}
	
	public static void main(String[] args) {
		JobManager jb = new JobManager()

		JCommander jcmd = new JCommander(jb);		
		JobRun run = new JobRun();
		jcmd.addCommand("run", run);			
		JobRun cmd // at this moment, only one command "run", later can "delete", etc.
		
		try {
			jcmd.parse(args)			
			String strCmd = jcmd.getParsedCommand()
			if (strCmd == null) {
				jcmd.usage()
				//System.exit(0)
			}
			
			if (strCmd.equals("run")) {
				cmd = run			
			} else {
				jcmd.usage();
				//System.exit(0);
			}			
			jb.run(cmd)
		} catch (ParameterException ex) {
			System.out.println(ex.getMessage());
			jcmd.usage();
			//System.exit(-1);
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("JobManager FAILED", ta)
			//System.exit(-2);
		}
		
		//System.exit(0);
	}
}
