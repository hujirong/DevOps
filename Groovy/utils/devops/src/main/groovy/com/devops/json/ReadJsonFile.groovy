package com.devops.json
import groovy.json.JsonSlurper
 
def jsonSlurper = new JsonSlurper()
data = jsonSlurper.parse(new File("payload.json")) 
println(data)