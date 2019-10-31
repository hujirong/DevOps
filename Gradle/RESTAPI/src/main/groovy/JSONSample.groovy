import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def a = new JsonSlurper().parse(new File("C:/Temp/abacus.JSON"))
JsonOutput.prettyPrint(a.toString())