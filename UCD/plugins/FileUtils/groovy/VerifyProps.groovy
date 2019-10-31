import com.cppib.core.pwdtool.PropertiesFile


/**
 * Verify property values.
 * @author fhou
 *
 */
class VerifyProps {
	static String SEC_PROP = '[SEC]'
	
	Properties inProps = new Properties()
	boolean DEBUG = false
	
	VerifyProps(String inPropFile) {
		new File(inPropFile).withInputStream { stream ->
			inProps.load(stream)
		}
		
		DEBUG = Boolean.parseBoolean(System.getProperty("SECDEBUG", "false"))
	}
	
	def run() {
		String fname = inProps['propFile']
		println "Verify property values for file $fname"

		PropertiesFile propFile = new PropertiesFile(fname)
		
		String propValues = inProps['propValues']
		
		propValues.eachLine { prop ->
			//split out the name
			def parts = prop.split("(?<=(^|[^\\\\])(\\\\{2}){0,8})=",2)
			def propName = parts[0].trim()
			def propValue = (parts.size() == 2 ? parts[1] : "").trim()
			
			//replace \, with just , and then \\ with \
			propName = propName.replace("\\=", "=").replace("\\,", ",") //.replace("\\\\", "\\")
			propValue = propValue.replace("\\=", "=").replace("\\,", ",") //.replace("\\\\", "\\")

			if (DEBUG) {
				println "Verify propName=[propName], propValue=[$propValue]"
			}
			
			boolean isSecure = false
			if (propName.startsWith(SEC_PROP)) {
				propName = propName.substring(SEC_PROP.length())
				isSecure = true
				
				if (DEBUG) {
					println "Secure property [$propName]"
				}
			}
			
			def fvalue = propFile.getProperty(propName, null, isSecure)
			if (propValue == fvalue) {
				println "Verified property $propName"
			} else {
				throw new Exception("Verify property $propName FAILED")
			}
		}
	}
	
	public static void main(String[] args) {
		VerifyProps vp = new VerifyProps(args[0])
		vp.run()	
	}
}
