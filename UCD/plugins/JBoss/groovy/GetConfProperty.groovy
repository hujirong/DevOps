
if (args.length != 3) {
	println "Usage GetConfProperty containerHome propertyName outputFile"
	System.exit(-2)
}
		
def JBossConfig conf = new JBossConfig()
conf.parse(args[0])

def propName = args[1]
def propValue = null

if (propName == 'CONF_DIR') {
	propValue = conf.confDir.canonicalPath
} else if (propName == 'DEPLOY_DIR') {
	propValue = conf.getJBossDeployDir().canonicalPath
} else {
	throw new Exception("Unsupported propName ${propName}")
}

// save output properties
Properties props = new Properties()
props.put('propValue', propValue)

def outStream = new FileOutputStream(args[2])
props.store(outStream, "")
outStream.close()
