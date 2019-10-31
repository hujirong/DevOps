import com.urbancode.air.AirPluginTool
import liquibase.integration.commandline.Main

def apTool = new AirPluginTool(this.args[0], this.args[1])

final def workDir = new File('.').canonicalFile
final def props = apTool.getStepProperties()
final def inputPropsFile = new File(args[0])
final def jarPath = System.getenv()['PLUGIN_HOME'] + "/lib/"

def cntFile = workDir.listFiles().findAll { it.name ==~ /.*.jar/ }.size()

if (cntFile==1) {
   workDir.eachFile { File f -> if (f.name  ==~ /.*.jar/ ) changeJarFile=f.getName() }
}
else
{
   throw new RuntimeException("Liquibase changelog jar file missing!");
}

def database = props['database']
def jdbcURL = props['jdbcURL']
def username = props['username']
def password = props['password']
def changeLogFile = props['changeLogFile']
def tag = props['tag']

def driver = null;
def driverClasspath = null;

if (database=='oracle') {
   driver = "oracle.jdbc.OracleDriver"
   driverClasspath = jarPath + "ojdbc6.jar:" + changeJarFile
}   
else if (database=='h2') {
   driver = "org.h2.Driver"
   driverClasspath = jarPath + "h2-1.4.187.jar:" + changeJarFile
}
else {
	 driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
   driverClasspath = jarPath + "sqljdbc4.jar:" + changeJarFile
}

def args = []
def i = 0
args[i++] = "--driver=" + driver
args[i++] = "--classpath=" + driverClasspath
args[i++] = "--url=" + jdbcURL
args[i++] = "--username=" + username
args[i++] = "--password=" + password
args[i++] = "--changeLogFile=" + changeLogFile
args[i++] = "--logLevel=debug"
args[i++] = "tag"
args[i++] = tag

println args

liquibase.integration.commandline.Main.main(args.toArray(new String[0]))
