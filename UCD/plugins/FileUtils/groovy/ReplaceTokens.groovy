
// Replace tokens in a text file (.properties, or .xml)
//

class ReplaceTokens {
	String startDelimiter
	String endDelimiter
	
	String envPropValues
		
	File targetFile
	
	ReplaceTokens(String inPropFile) {
		// read parameters from args[0]
		def props = new Properties()
		
		def inputPropsFile = new File(inPropFile)
		inputPropsFile.withInputStream { stream ->
			props.load(stream)
		}

		init(props)		
	}
	
	ReplaceTokens(Properties inProps) {
		init(inProps)
	}
	
	def init(inProps) {
		def fileName = inProps['file']
		
		startDelimiter = inProps['startDelimiter']
		endDelimiter = inProps['endDelimiter']
		
		envPropValues = inProps['envPropValues']
		
		targetFile = new File(fileName)
	}
	
	def run() {
		// filter file contains all properties passed from envPropValues
		def filterFile = new File('filter_' + System.currentTimeMillis())

		try {
			def tempProps = new Properties()
			
			println "Parse envPropValues ..."
			
		    //this is jeffs magic regex to split on ,'s preceded by even # of \ including 0
		    envPropValues.split("(?<=(^|[^\\\\])(\\\\{2}){0,8}),").each { prop ->
				//split out the name
				def parts = prop.split("(?<=(^|[^\\\\])(\\\\{2}){0,8})=",2)
				def propName = parts[0]
				def propValue = parts.size() == 2 ? parts[1] : ""
				
				//replace \, with just , and then \\ with \
				propName = propName.replace("\\=", "=").replace("\\,", ",") //.replace("\\\\", "\\")
				propValue = propValue.replace("\\=", "=").replace("\\,", ",") //.replace("\\\\", "\\")
		
				tempProps.setProperty(startDelimiter + propName + endDelimiter, propValue)
			}
		
		    filterFile.withOutputStream { outStream ->
		        tempProps.store(outStream, 'Auto generated property file')
		    }
		
			println "Replace tokes in file ${targetFile.canonicalPath}"
			
		    def ant = new AntBuilder()
		    ant.replace(file: targetFile.canonicalPath,
					summary: 'true',
					replacefilterfile: filterFile.canonicalPath)
		} catch (Exception e) {
		    e.printStackTrace()
		    println "Error replacing tokens!"
		    System.exit(1)
		} finally {
			filterFile.delete()
		}
	}
	
	static void main(String[] args) {
		ReplaceTokens rt = new ReplaceTokens(args[0])
		rt.run()
	}
}
