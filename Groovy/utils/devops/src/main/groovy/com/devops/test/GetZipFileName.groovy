package com.devops.test

import groovy.io.FileType

// def outProps = [:]

def currentDir = new File('.')
def targetDir = null

currentDir.eachFile(FileType.FILES) {
    if (it.name.endsWith('.zip')) {
		println("Find file ${it.name}")
		targetDir = it.name - '_bin.zip'
    }
}

println("targetDir=$targetDir")
assert targetDir != null

//outProps.put("zipfilename", targetDir)