/**
 * JBoss container configuration
 * @author fhou
 *
 */
class JBossConfig {
	int version
	
	File confDir
	File binDir
	
	String containerHome
	
	String jbossHome
	String containerName
	
	void parse(String containerHome) {
		this.containerHome = containerHome
		
		// get jboss_version
		if (containerHome.startsWith('/opt/jboss5')) {
			version = 5
			_parseJBoss5()
		} else if (containerHome.startsWith('/opt/jboss7')) {
			version = 7
			_parseJBoss7()
		} else if (containerHome.startsWith('/opt/jboss8')) {
			version = 8
			_parseJBoss8()
		} else {
			throw new Exception("Cannot figure out jboss version")
		}
	}
	
	void _parseJBoss5() {
		def lstr = '/server/'
		assert (containerHome.contains(lstr)) : "Container home $containerHome does not contain string $lstr"
		
		def items = containerHome.split(lstr)
		jbossHome = items[0]
		_setContainerName(items[1])
		
		confDir = new File(containerHome, 'conf')
		binDir = new File(jbossHome, 'bin')
	}
	
	void _parseJBoss7() {
		def lstr = '/cppib/'
		assert (containerHome.contains(lstr)) : "Container home $containerHome does not contain string $lstr"
		
		def items = containerHome.split(lstr)
		jbossHome = items[0]
		_setContainerName(items[1])
		
		confDir = new File(containerHome, 'configuration')
		binDir = new File(containerHome, 'bin')

	}
	
	void _parseJBoss8() {
		_parseJBoss7()
	}
	
	void _setContainerName(containerName) {
		while (containerName[0] == '/')
			containerName = containerName[1..-1]

		while (containerName[-1] == '/')
			containerName = containerName[0..-2]
			
		this.containerName = containerName	
	}
	
	File getJBossDeployDir() {
		if (version == 5) {
			return new File(containerHome, "${containerName.toUpperCase()}-deploy")
		}
		
		return new File(containerHome, "deployments")
	}
	
	File getJBossConfFile() {
		File confFile = new File(binDir, "run${containerName.toUpperCase()}.conf")
		assert confFile.exists()
		return confFile
	}
	
	File getInitFile() {
		File initFile = new File(binDir, "jboss.${containerName.toUpperCase()}.init")
	}
	
	// get JBoss 7/8 standalone XML file
	File getStandaloneFile() {
		assert (version == 7 || version == 8)

		String fname
		
		String mark = "{JBOSS_PROFILE:-"
		File initFile = getInitFile()
		initFile.eachLine { line ->
			
			if (line.contains(mark)) {
				fname = line[line.indexOf(mark) + mark.length() .. -2]
				if (fname[0] == '"')
					fname = fname[1 .. -1]
					
				if (fname[-1] == '"')
					fname = fname[0 .. -2]
			}
		}
				
		File f = new File(confDir, fname)
		assert f.exists()
		return f
	}
}
