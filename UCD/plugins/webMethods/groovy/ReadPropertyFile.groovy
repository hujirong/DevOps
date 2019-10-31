

if (args.length != 2) {
	println "Usage: groovy ReadPropertyFile.groovy PropertyFile outPropsFile"
	System.exit(1)
}

println "Read properties file ${args[0]}"

Properties props = new Properties()
new File(args[0]).withInputStream {
	stream -> props.load(stream)
}

new File(args[1]).withOutputStream {
	stream -> props.store(stream, "")
}
