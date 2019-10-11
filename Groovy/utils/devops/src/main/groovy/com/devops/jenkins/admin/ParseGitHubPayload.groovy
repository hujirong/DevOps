package com.devops.jenkins.admin
import groovy.json.JsonSlurper
import com.devops.github.model.*

class ParseGitHubPayload {
    private Payload payload
    void process() {
        List commits = payload.getCommits()
        String[] added=[], modified=[], removed=[], changed=[]
        for(item in commits){
            Commit commit = new Commit(item)
            assert commit instanceof Commit
            if (commit) {
                added = added + commit.getAdded()
                modified = modified + commit.getModified()
                removed = removed + commit.getRemoved()
            }
        }
        //println "added:" + added
        //println "modified:" + modified
        //println "removed:" + removed
        changed = added + modified + removed
        //println "total changed:" + changed.toString()
        List changedList = changed.toString().split(',')
        println changedList.sort().unique()
        
    }

    public static void main(String[] args) {
        def payloadJson = new File("payload_unix_pr.json")
        def payloadMap = new JsonSlurper().parseFile(payloadJson, 'UTF-8')
        assert payloadMap instanceof Map

        ParseGitHubPayload parseGitHubPayload = new ParseGitHubPayload()
        parseGitHubPayload.payload = new Payload(payloadMap)
        parseGitHubPayload.process()
    }
}