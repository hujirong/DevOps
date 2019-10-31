package com.devops.test
import java.nio.file.Path

def changes = []
new File( 'spica/script/change_list' ).eachLine { line ->
	changes << line
}
String sourceDir = "spica"
String backupDir = "C:/Temp/spica_backup"
String deployDir = "C:/Temp/spica"

// Backup
def ant = new AntBuilder()
ant.copy(todir: backupDir) {
	fileset(dir: sourceDir)
}

//Deploy
changes.each {
	println it
	String[] fn = it.split("\\,");
	if (( fn[0] =="M" ) | ( fn[0] =="A" )) {
		ant.copy( file:"$sourceDir${fn[1]}", tofile:"$deployDir${fn[1]}")
		println "${fn[1]} copied"
	}
	else if ( fn[0] =="D" ) {
		String filename = deployDir + fn[1]		
		assert  new File(filename).delete()
		println "${fn[1]} deleted"
	}
	
}