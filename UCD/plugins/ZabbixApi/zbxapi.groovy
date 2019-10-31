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

def proc; // Background process handle

def application; // Maintenance application
def description; // Maintenance description
def trigger; // Trigger name 

def exit = 0; // Default exit code

def exitCode; // Exit code from Zabbix API

def last = 0; // Last time a trigger or item was updated (epoch)

def env = []; // Environment for background processes

def timer = 0; // Global timer for timed trigger and item checks
def tmcmd = []; // Timeout command

def item; // Item name
def value; // Zabbix tirgger/item value

def command // Zabbix API command line

def start = new Date();
def stop = start;

def maxtime = 3600 * 1000; // Maximum time to wait for a trigger or item
def naptime = 30 * 1000; // Time to wait between successive checks for triggers and items

def unixepoch = Math.round(System.currentTimeMillis() / 1000); // Current (Epoch) time in milliseconds

def loop = "TRUE"; // Process multiple triggers

def ucstep    = props['Step']; // UC Step variable
def username  = props['Username']; // UC Username variable
def password  = props['Password']; // UC Password variable
def hostnames = props['Hostname']; // UC Hostname variable
def timeout   = props['Timeout']; // Wait for Zabbix API
def triggers  = props['Trigger']; // Trigger name

triggers = (triggers != null ? triggers : "")

println "Step : " + ucstep;
println "Username : " + username;
println "Timeout : ${timeout?:'0'}";
println "Epoch : ${unixepoch?:''}";
println "Naptime : ${naptime?:''}";

System.getenv().each {
  env << it.key + '=' + it.value
}
  
env << "ZBXAPI_USERNAME=$username";
env << "ZBXAPI_PASSWORD=$password";
  
for (hostname in hostnames.split()) {

  println "Hostname : " + hostname;

  loop = "TRUE";

  for (def triggerList = triggers.tokenize("\r\n"); loop || triggerList.size() > 0; loop = (ucstep == 'TriggerState')) {
  
    command = ["/usr/local/bin/zabbysh"];
  
    println "Step : " + ucstep;

    switch (ucstep) {
    
      case 'MaintenanceStart':
  
        use(TimeCategory) {
            stop = start + 1.day;
        }
    
        application = props['Application'];
        description = props['Description'];
    
        command << "/etc/zabbix/maintenance.zby";
        command << "${application?:${hostname}}/${hostname}";
        command << hostname;
        command << start;
        command << stop;
        command << description;
    
        break;
    
      case 'MaintenanceStop':
    
        application = props['Application'];
    
        command << "/etc/zabbix/maintenance.zby";
        command << "${application?:${hostname}}/${hostname}";
    
        break;
    
      case 'Trigger':
      case 'TriggerState':
    
        trigger = triggerList.pop();

        if (trigger.trim().size() == 0) {
          continue;
        }
  
        value   = (ucstep == 'TriggerState' ? 'value' : props['Value']);
    
        if (timeout.isInteger()) {
          timer = timeout.toInteger();
        }
    
        println "Trigger : ${trigger?:''}";
        println "Value : ${value?:''}";
        println "Timeout : ${timeout?:'0'}";
    
        command << "/etc/zabbix/trigger.zby";
        command << hostname;
        command << "${trigger?:''}";
    
        if (timer > 0) {
          tmcmd = command;
          tmcmd << 'lastchange';
        }
    
        command << "${value?:''}";
    
        break;
    
      case 'Item':
    
        item    = props['Item'];
        value   = props['Value'];
    
        if (timeout.isInteger()) {
          timer = timeout.toInteger();
        }
    
        println "Item : ${item?:''}";
        println "Value : ${value?:''}";
        println "Timeout : ${timeout?:'0'}";
    
        command << "/etc/zabbix/item.zby";
        command << hostname;
        command << "${item?:''}";
    
        if (timer > 0) {
          tmcmd = command;
          tmcmd << 'lastclock';
        }
    
        command << "${value?:''}";
    
        break;
    
      default:
    
        throw new RuntimeException("Unknown step!");
    }
    
    // Handle timed trigger and items checks here ...
    
    timer = Math.min((timer * 1000), maxtime);
    
    println "Timer : ${timer?:'0'}";
    
    for (int nap = timer; nap > 0; nap -= naptime) {
      println "Nap : ${nap?:''}";
      proc = tmcmd.execute(env, workDir);
      last = proc.getText();
      println "Last : ${last?:''}";
      proc.waitForOrKill(naptime);
      if ((last.isInteger()?last.toInteger():unixepoch) < unixepoch) {
        sleep(naptime);
        continue;
      }
      break;
    }
    
    // Run the Zabbix API command here ...
    
    proc = command.execute(env, workDir);
    
    if (ucstep == 'TriggerState') {
      exitCode = proc.getText();
      exit |= exitCode.isInteger() ? exitCode.toInteger() : 1;
    } else {
      proc.waitForProcessOutput(System.out, System.out);
    }
    
    proc.waitForOrKill(naptime);
    
    if (proc.exitValue() != 0) {
      throw new RuntimeException("Process Failed!");
    }
  }
}

System.exit(exit);

