import com.cppib.core.pwdtool.PropertiesFile


/**
 * Add/update property values in properties file, and encrypt the secure value.
 * @author fhou
 *
 */
class SetProps {
	static String SEC_PROP = '[SEC]'
	
	Properties inProps = new Properties()
	
	SetProps(String inPropFile) {
		new File(inPropFile).withInputStream { stream ->
			inProps.load(stream)
		}
	}
	
	def run() {
		String fname = inProps['propFile']
		println "Set properties for file $fname"

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

			boolean isSecure = false
			if (propName.startsWith(SEC_PROP)) {
				propName = propName.substring(SEC_PROP.length())
				isSecure = true
			}
			
			println "Set propName=$propName, isSecure=$isSecure"
			propFile.setProperty(propName, propValue, isSecure)
		}
	}
	
	public static void main(String[] args) {
		SetProps sp = new SetProps(args[0])
		sp.run()	
	}
}
