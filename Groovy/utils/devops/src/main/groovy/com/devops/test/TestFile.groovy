package com.devops.test
String directory = "."
String filename;

filename=new File(directory).listFiles().find{it.name.endsWith(".zip")}
println "filename=$filename"

filename=new File(directory).listFiles().find{it.name.endsWith(".zip")}.name - ".zip"
println "filename=$filename"