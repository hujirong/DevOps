package com.devops.test

import groovy.io.FileType

def currentDir = new File('.')
def files = []
currentDir.eachFile(FileType.FILES) {
	if (it.name.endsWith(".zip")) {
		files << it.name - "_bin.zip"
	}
}

println files[0]
outProps.put("zipfilename", files[0])