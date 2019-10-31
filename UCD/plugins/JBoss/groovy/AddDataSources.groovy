

/*
 * Add datasources to JBoss application
 */
class AddDataSources {
	String containerHome
	JBossConfig jbossConfig
	
	String dsFileName
	File dsFile
	
	boolean encryptPassword
	
	boolean isUnix = !System.properties['os.name'].toLowerCase().contains('windows')
	
	void processDsJBoss5() {
		File deployDir = jbossConfig.getJBossDeployDir()
		File deployedFile = new File(deployDir, dsFile.name)
		
		backupAndUpdateJBoss5(deployedFile, dsFile)
	}
	
	void backupAndUpdateJBoss5(File deployedFile, File dsFile) {
		def ant = new AntBuilder()
		
		if (!deployedFile.exists()) {
			println "Copy file $dsFile to $deployedFile"
			ant.copy(file: dsFile, tofile: deployedFile)
			
			if (isUnix)
				ant.chmod(file: deployedFile, perm: "600")

			return	
		}
		
		// check if file changes
		def f1 = deployedFile.text
		def f2 = dsFile.text
		
		if (f1 == f2) {
			println "No need to update ${deployedFile}"
			return
		}
		
		File backupDir = new File(deployedFile.parent, 'ucdbak')
		if (!backupDir.exists()) {
			backupDir.mkdirs()
		}
		
		File backupFile = new File(backupDir, deployedFile.name + '.' + System.currentTimeMillis())
		
		println "Backup file $deployedFile to $backupFile"
		ant.move(file: deployedFile, tofile: backupFile)
		
		if (isUnix)
			ant.chmod(file: backupFile, perm: "600")
		
		println "Update file $deployedFile"
		ant.copy(file: dsFile, tofile: deployedFile)
		
		
		if (isUnix)
			ant.chmod(file: deployedFile, perm: "600")
	}
	
	void processDsJBoss7() {
		// load datasources
		def dslist = new XmlParser().parse(dsFile)
		
		File stdFile = jbossConfig.getStandaloneFile()
		
		File tmpFile = writeStandaloneJBoss7(stdFile, dslist)
		backupAndUpdateJBoss7(stdFile, tmpFile)
	}
	
	File writeStandaloneJBoss7(File stdFile, dslist) {
		File tmpFile = new File(stdFile.parentFile, stdFile.name + '.' + System.currentTimeMillis())
		
		tmpFile.withWriter { Writer writer ->
			String DS = "NO"
			 
			stdFile.eachLine { line ->
				if (line.contains('<datasources>')) {
					DS = "DS0"
				} else if (line.contains('</datasources>')) {
					DS = "DS2"
				}
				
				if (DS == "NO" || DS == "DONE") {
					writer << line << '\n'
				} else if (DS == "DS0") {
					def sw = new StringWriter()
					def xmlNodePrinter = new XmlNodePrinter(new PrintWriter(sw))
					xmlNodePrinter.with {
						preserveWhitespace = true
						expandEmptyElements = true
					}
					
					xmlNodePrinter.print(dslist)
					
					writer << sw.toString()
					DS = "DS1"
				} else if (DS == "DS2") {
					DS = "DONE"
				}
			}
		}
		
		return tmpFile
	}
	
	void backupAndUpdateJBoss7(File stdFile, File tmpFile) {
		// check if file changes
		def f1 = stdFile.text
		def f2 = tmpFile.text
		
		if (f1 == f2) {
			println "No need to update ${stdFile}"
			tmpFile.delete()
			return
		}
		
		File backupDir = new File(stdFile.parent, 'ucdbak')
		if (!backupDir.exists()) {
			backupDir.mkdirs()
		}
		
		File backupFile = new File(backupDir, tmpFile.name)
		
		def ant = new AntBuilder()
		
		println "Backup file $stdFile to $backupFile"
		ant.copy(file: stdFile, tofile: backupFile)
		
		if (isUnix)
			ant.chmod(file: backupFile, perm: "600")
		
		println "Update file $stdFile"
		ant.move(file: tmpFile, tofile: stdFile, overwrite: true)
		
		if (isUnix)
			ant.chmod(file: stdFile, perm: "600")
	}
	
	void replaceDsInStandalone(ds, stdRoot) {
		def stdDsList = stdRoot.profile.subsystem.datasources
		
		def stdDs = stdDsList.datasource.find { it['@jndi-name'] == ds['@jndi-name'] }
		if (stdDs) {
			stdDsList.remove(stdDs)
		}
		
		stdDsList.add(ds)
	}
	
	void process() {
		println "Add datasources: containerHome is ${containerHome}"
		println "                 datasourceFile is ${dsFileName}"

		dsFile = new File(dsFileName)
		assert dsFile.exists() : "File ${dsFileName} is not exist"
		
		jbossConfig = new JBossConfig()
		jbossConfig.parse(containerHome)
		
		if (jbossConfig.version == 5) {
			processDsJBoss5()
		} else if (jbossConfig.version == 7) {
			processDsJBoss7()
		} else if (jbossConfig.version == 8) {
			processDsJBoss7()
		}
	}
	
	static void main(String[] args) {
		if (args.length != 3) {
			println "Usage AddDataSources containerHome datasourceFile encryptPassword"
			System.exit(-2)
		}
		
		AddDataSources ds = new AddDataSources(containerHome: args[0], dsFileName: args[1], encryptPassword: args[2])
		ds.process()
	}
}
