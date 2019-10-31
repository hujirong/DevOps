import java.util.*;
import java.io.*;

import org.codehaus.groovy.runtime.TimeCategory;

final def workDir = new File('.').canonicalFile;
final def props = new Properties();
final def inputPropsFile = new File(args[0]);

try {
    inputPropsStream = new FileInputStream(inputPropsFile);
    props.load(inputPropsStream);
} catch (IOException e) {
    throw new RuntimeException(e);
}

def appName   = props['appName']; // UC application name
def envName   = props['envName']; // UC environment name
def username  = props['Username']; // UC Username variable
def password  = props['Password']; // UC Password variable
def hostname  = props['hostname']; // UC Hostname variable
def description = props['Description']; // UC Description variable
def checkfile   = props['checkfile'];   // Zabbix security check file

File securityFile = new File(checkfile)

for (hostitem in hostname.split()) {
    chkstr = appName + "|" + envName + "|" + hostitem
    if (!securityFile.text.contains(chkstr)) {
        throw new RuntimeException("Invalid Hostname:" + hostitem);
    }
}

def command // Zabbix API command line
def env = []; // Environment for background processes
System.getenv().each {
  env << it.key + '=' + it.value
}
  
env << "ZBXAPI_USERNAME=$username";
env << "ZBXAPI_PASSWORD=$password";

def start = new Date();
def stop = start;

use(TimeCategory) {
            stop = start + 1.day;
        }

for (hostitem in hostname.split()) {
    command = ["/usr/local/bin/zabbysh"];
    command << "/etc/zabbix/maintenance.zby";
    command << "${appName}/${hostitem}";
    command << hostitem;
    command << start;
    command << stop;
    command << description;
    
    proc = command.execute(env, workDir);
    proc.waitForProcessOutput(System.out, System.out);

    if (proc.exitValue() != 0) {
      throw new RuntimeException("FAIL --- start maintenance for hostname:" + hostitem );
    }
    else {
      println "SUCCESS --- start maintenance for hostname:" + hostitem ;
    }
    
}
