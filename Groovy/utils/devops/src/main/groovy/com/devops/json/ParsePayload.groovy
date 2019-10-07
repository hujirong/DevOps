package com.devops.json
import groovy.json.JsonSlurper
import com.devops.github.model.*

//def inputFile = new File("payload.json")
//def InputJSON = new JsonSlurper().parseText(inputFile.text)

def payload = new File("payload.json")
def payloadMap = new JsonSlurper().parseFile(payload, 'UTF-8')
assert payloadMap instanceof Map
println "ref:" + payloadMap.ref  //ref:refs/heads/TST

def commits = payloadMap.commits
assert commits instanceof List

def addeds = payloadMap.commits.added
assert addeds instanceof List
println "addeds:" + addeds  //addeds:[[], []]
def removeds = payloadMap.commits.removed
assert removeds instanceof List
println "removeds:" + removeds  //removeds:[[], []]
def modifieds = payloadMap.commits.modified
assert modifieds instanceof List
println "modifieds:" + modifieds  //modifieds:[[AOL/ASM/CX/JIRONG.ASM], [AOL/ASM/CX/JIRONG.ASM]]

def changed = addeds + removeds + modifieds
assert changed instanceof List
println "changed:" + changed  //changed:[[], [], [], [], [AOL/ASM/CX/JIRONG.ASM], [AOL/ASM/CX/JIRONG.ASM]]

println "uniquelly sorted non-empty changed:" + changed.sort().unique()  //uniquelly sorted non-empty changed:[[], [AOL/ASM/CX/JIRONG.ASM]]

println "non-empty changed:" + changed.remove(1)  //non-empty changed:[AOL/ASM/CX/JIRONG.ASM]


