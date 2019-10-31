import java.util.*;
import java.io.*;

import org.codehaus.groovy.runtime.TimeCategory;

final def workDir = new File('.').canonicalFile;
final def props = new Properties();
final def inputPropsFile = new File(args[0]);
final def maxtime = 3600 * 1000; // Maximum time to wait for a trigger

try {
    inputPropsStream = new FileInputStream(inputPropsFile);
    props.load(inputPropsStream);
} catch (IOException e) {
    throw new RuntimeException(e);
}

def username  = props['Username']; // UC Username variable
def password  = props['Password']; // UC Password variable
def hostname  = props['hostname']; // UC Hostname variable
def trigger   = props['trigger'];  // UC trigger variable
def timeout   = props['timeout'];  // UC timeout variable

if (timeout.isInteger()) {
    timer = timeout.toInteger();
}
else {
    timer = 0
}

timer = Math.min((timer * 1000), maxtime);

def command // Zabbix API command line
def env = []; // Environment for background processes
System.getenv().each {
  env << it.key + '=' + it.value
}
  
env << "ZBXAPI_USERNAME=$username";
env << "ZBXAPI_PASSWORD=$password";

//Validate trigger is enabled
command = ["/usr/local/bin/zabbysh"];
command << "/etc/zabbix/trigger.zby";
command << hostname;
command << trigger;
command << 'status';

def out = new StringBuffer()
def err = new StringBuffer()

proc = command.execute(env, workDir);
proc.waitForProcessOutput(out, err);

if (proc.exitValue() != 0) {
    throw new RuntimeException("FAIL --- cannot get trigger:" + trigger );
} 
else {
    if ( out.size() == 0 ) {
         throw new RuntimeException("FAIL --- trigger:" + trigger + " is not exist");
    }

    def resultCode 
    for ( outitem in out.split() ) {
        resultCode = outitem
    }
    if (resultCode == '1' ) {
       throw new RuntimeException("FAIL --- trigger:" + trigger + " is disabled"); 
    }
}

//Validate trigger is in good state(i.e. not unknown)
command = ["/usr/local/bin/zabbysh"];
command << "/etc/zabbix/trigger.zby";
command << hostname;
command << trigger;
command << 'state';

proc = command.execute(env, workDir);
proc.waitForProcessOutput(out, err);

if (proc.exitValue() != 0) {
    throw new RuntimeException("FAIL --- cannot get trigger:" + trigger );
}
else {
    if ( out.size() == 0 ) {
         throw new RuntimeException("FAIL --- trigger:" + trigger + " is not exist");
    }

    def resultCode
    for ( outitem in out.split() ) {
        resultCode = outitem
    }
    if (resultCode == '1' ) {
       throw new RuntimeException("FAIL --- trigger:" + trigger + " is in bad status");
    }
}

sleep(timer)

command = ["/usr/local/bin/zabbysh"];
command << "/etc/zabbix/trigger.zby";
command << hostname;
command << trigger;
command << 'value';

proc = command.execute(env, workDir);
proc.waitForProcessOutput(out, err);

if (proc.exitValue() != 0) {
    throw new RuntimeException("FAIL --- cannot get trigger:" + trigger );
}
else {
    if ( out.size() == 0 ) {
         throw new RuntimeException("FAIL --- trigger:" + trigger + " is not exist");
    }

    def resultCode
    for ( outitem in out.split() ) {
        resultCode = outitem
    }
    if (resultCode == '1' ) {
        throw new RuntimeException("FAIL --- trigger:" + trigger );
    }
    else {
        println "SUCCESS --- trigger:" + trigger 
    }
}

