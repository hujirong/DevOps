package com.devops.json
import groovy.json.JsonSlurper
import com.devops.github.model.*

//def inputFile = new File("payload.json")
//def InputJSON = new JsonSlurper().parseText(inputFile.text)

def payload = new File("payload_unix_pr.json")
def payloadMap = new JsonSlurper().parseFile(payload, 'UTF-8')
assert payloadMap instanceof Map
println "ref:" + payloadMap.ref  //ref:refs/heads/TST

def commits = payloadMap.commits
assert commits instanceof List

// process added files
def addeds = payloadMap.commits.added
assert addeds instanceof List
println "size:" + addeds.size()  // 1
println "addeds:" + addeds  //addeds:[[], []]
String addedString = addeds.toString().replace("[", "").replace("]", "")
println "addedString:" + addedString
def addedList
if (addedString) {
	addedList = addedString.split(',')
	println "addedList:" + addedList
	assert addedList instanceof String[]
}

// process removed files
def removeds = payloadMap.commits.removed
println "removeds:" + removeds  //removeds:[[], []]
String removedString = removeds.toString().replace("[", "").replace("]", "")
def removedList
if (removedString) {
	removedList = removedString.split(',')
	println "removedList:" + removedList
	assert removedList instanceof String[]
}

// process modified files
def modifieds = payloadMap.commits.modified
println "modifieds:" + modifieds  //modifieds:[[AOL/ASM/CX/JIRONG.ASM], [AOL/ASM/CX/JIRONG.ASM]]
String modifiedString = modifieds.toString().replace("[", "").replace("]", "")
def modifiedList
if (modifiedString) {
	modifiedList = modifiedString.split(',')
	println "modifiedList:" + modifiedList
	assert modifiedList instanceof String[]
}

// consolidate all changed files list
def changedList
changedList = addedList + removedList + modifiedList
println "changedList:" + changedList

def changed = changedList.findAll()
// remove null & empty
println "remove null & empty changed:" + changed

println "uniquelly sorted changed:" + changed.sort().unique()



