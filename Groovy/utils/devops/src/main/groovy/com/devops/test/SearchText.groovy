package com.devops.test
def file = new File('build.log')
//def pat = /.*-bin\.zip.*/
def pat = /BUILD SUCCESSFUL/

def result = []
def idx = -1
def str =

file.eachLine { line, count ->
	if (line =~ pat) {
		result << [count, line]
	}
}

if (result.size ==0 ) {
	println("text not found")
} else {
	result.each {
		println("${it[0]}, ${it[1]}")
	}
}
