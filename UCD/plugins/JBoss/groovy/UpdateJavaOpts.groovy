/**
 * Update JAVA_OPTS in JBoss conf file
 */

class UpdateJavaOpts {
	static String JBOSS_CONF_DIR = '-DJBOSS_CONF_DIR'
	
	static String removeQuote(String str) {
		str = str.trim()
		if (str[0] == '"') {
			str = str[1 .. -1]
		}
		
		if (str[-1] == '"') {
			str = str[0 .. -2]
		}

		return str
	}
	
	static class JvmOption {
		String key = ''
		String value = ''
		int lineNo
		boolean changed = false
		
		String getOption() {
			if (key == '-Xms' || key == '-Xmx' || key == '-Xss') {
				return key + value
			}
			
			if (value == '') {
				return key
			}
			
			return key + '=' + value
		}
		
		boolean changeValue(String value) {
			if (this.value != value) {
				this.value = value
				changed = true
			}
			
			return changed
		}
		
		static createJvmOption(String option, int lineNo = -1) {
			JvmOption opt = new JvmOption()
			opt.lineNo = lineNo
			
			if (option.startsWith('-Xms') || option.startsWith('-Xmx') || option.startsWith('-Xss')) {
				opt.key = option[0 .. 3]
				opt.value = option[4 .. -1]
			} else {
				int idx = option.indexOf('=')
				assert (idx != 0) : "No name for option ${option}"
				
				if (idx < 0) {
					opt.key = option
				} else {
					opt.key = option[0 ..< idx].trim()
					opt.value = option[(idx + 1) .. -1].trim()
				}
			}
			
			return opt
		}
	}
	
	static class JvmOptsLine {
		int lineNo
		String prefix
		List<JvmOption> options = []
		
		boolean isChanged() {
			boolean changed = false
			
			options.each { it ->
				if (it.changed) {
					changed = true
				}
			}
			
			return changed
		}
		
		JvmOption findJvmOption(String key) {
			JvmOption opt = null
			
			options.find { it ->
				if (it.key == key) {
					opt = it
					return true
				}
			}
			
			return opt
		}
		
		boolean addOption(JvmOption opt) {
			JvmOption opt1 = new JvmOption()
			opt1.key = opt.key
			opt1.value = opt.value
			opt1.lineNo = lineNo
			opt1.changed = true
			options.add(opt1)
			
			return opt1.changed
		}
		
		String getLine() {
			StringBuilder sb = new StringBuilder()
			sb.append(prefix)
			
			sb.append('"')
			boolean first = true
			
			options.each { opt ->
				if (first) {
					first = false
				} else {
					sb.append(' ')
				}
				
				sb.append(opt.getOption())
			}
			
			sb.append('"')
			return sb.toString()
		}
		
		static JvmOptsLine parseLine(String line, int lineNo) {
			JvmOptsLine optsLine = new JvmOptsLine()
			optsLine.lineNo = lineNo
			
			int idx = line.indexOf('=')
			optsLine.prefix = line[0 .. idx]
			
			def sln = line[(idx + 1) .. -1]
			sln = UpdateJavaOpts.removeQuote(sln)

			def items = sln.split(" ")
			items.each { String opt ->
				opt = opt.trim()
				if (opt.length() > 0) {
					JvmOption jvmopt = JvmOption.createJvmOption(opt, lineNo)
					optsLine.options.add(jvmopt)
				}
			}
			
			return optsLine
		}
	}
	
	// input parameters
	String javaOpts
	String containerHome
	
	JBossConfig jbossConfig
	File confFile
	
	List<JvmOptsLine> lineOpts = []
	
	// load options into confOptMap and confOptList
	def loadConfFile() {
		confFile.eachLine { line, lineNo ->
			def sln = line.trim()
			if (sln.startsWith("JAVA_OPTS=")) {
				JvmOptsLine lineOpt = JvmOptsLine.parseLine(line, lineNo)
				lineOpts.add(lineOpt)
			}
		}
	}
	
	String getJvmOptLine(String line, int lineNo) {
		String newLine = null
		lineOpts.find { lineOpt ->
			if (lineOpt.lineNo == lineNo) {
				if (lineOpt.isChanged()) {
					newLine = lineOpt.getLine()
				}
				
				return true
			}
		}
		
		if (newLine != null) {
			return newLine
		}
		
		return line
	}
	
	def updateConfFile() {
		File backupDir = new File(confFile.parent, 'ucdbak')
		if (!backupDir.exists()) {
			backupDir.mkdirs()
		}
		
		
		File backupFile = new File(backupDir, confFile.name + '.' + System.currentTimeMillis())
		
		def ant = new AntBuilder()
		
		println "Backup file $confFile to $backupFile"
		ant.copy(file: confFile, tofile: backupFile)
		
		println "Update file $confFile"
		File tmpFile = new File(confFile.parent, backupFile.getName())
		
		tmpFile.withWriter { Writer writer ->
			confFile.eachLine { line, lineNo ->
				def updatedLine = getJvmOptLine(line, lineNo)
				writer << updatedLine
				writer << '\n'
			}
		}
		
		ant.move(file: tmpFile, tofile: confFile, overwrite: true)
	}

	boolean processOption(JvmOption opt) {
		boolean found = false
		boolean changed = false
		
		lineOpts.reverseEach { lineOpt ->
			if (!found) {
				JvmOption jvmOption = lineOpt.findJvmOption(opt.key)
				if (jvmOption != null) {
					found = true
					changed = jvmOption.changeValue(opt.value)
				}
			}
		}
		
		if (!found) {
			changed = lineOpts[0].addOption(opt)
		}
		return changed
	}
	
	
	def process() {
		println "Process JBoss JAVA_OPTS: $javaOpts"
		println "JBoss container HOME: $containerHome"
		
		jbossConfig = new JBossConfig()
		jbossConfig.parse(containerHome)
		confFile = jbossConfig.getJBossConfFile()
		
		println "Process confFile: $confFile"
		loadConfFile()
		
		boolean needUpdate = false
		
		// process each option
		javaOpts = removeQuote(javaOpts)
		def items = javaOpts.split(" ")
		items.each { String item ->
			JvmOption jvmOpt = JvmOption.createJvmOption(item)
			
			if (processOption(jvmOpt)) {
				needUpdate = true
			}
		}

		// always add JBOSS_CONF_DIR
		String confDir = JBOSS_CONF_DIR + '=' + jbossConfig.confDir.canonicalPath
		JvmOption confDirOpt = JvmOption.createJvmOption(confDir)
		if (processOption(confDirOpt)) {
			needUpdate = true
		}
		
		if (needUpdate) {
			updateConfFile()
		} else {
			println "No need to update file $confFile"
		}
	}
	
	static void main(String[] args) {
		if (args.length != 2) {
			println "Usage UpdateJavaOpts containerHome javaOpts"
			System.exit(-2)
		}
		
		UpdateJavaOpts ujo = new UpdateJavaOpts(containerHome: args[0], javaOpts: args[1])
		ujo.process()
	}
}