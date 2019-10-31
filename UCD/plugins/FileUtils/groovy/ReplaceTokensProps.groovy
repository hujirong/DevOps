import com.cppib.core.pwdtool.PropertiesFile

/**
 * Replace tokens in properties file and encrypt secure values
 * @author fhou
 *
 */
class ReplaceTokensProps {
	Properties inProps  = new Properties()
	
	ReplaceTokensProps(String inPropFile) {
		def inputPropsFile = new File(inPropFile)
		inputPropsFile.withInputStream { stream ->
			inProps.load(stream)
		}
	}

	def run() {
		// replace tokens
		inProps['file'] = inProps['propFile']
		
		ReplaceTokens rtks = new ReplaceTokens(inProps)
		rtks.run()
		
		// encrypt secure properties
		File targetFile = rtks.targetFile
		String secureProps = inProps['secureProperties'] 
		
		encryptProperties(targetFile, secureProps)
	}	

	def encryptProperties(File targetFile, String secureProps) {
		println "Encrypt secure values in ${targetFile.canonicalPath}"
		
		PropertiesFile propFile = new PropertiesFile(targetFile)
		
		secureProps.eachLine { propName ->
			propName = propName.trim()
			
			if (propName.length() > 0) {
				println "Encrypt value for [$propName]"
				
				String val = propFile.getProperty(propName)
				if (val == null) {
					throw new Exception("Property $propName is not defined in file ${targetFile.canonicalPath}")
				}
				
				println "Set secure property $propName"
				propFile.setSecureProperty(propName, val)
			}
		}
	}	
	
	public static void main(String[] args) {
		ReplaceTokensProps rtkProps = new ReplaceTokensProps(args[0])
		rtkProps.run()
	}
}
