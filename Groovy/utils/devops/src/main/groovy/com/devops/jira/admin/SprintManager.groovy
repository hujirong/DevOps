package com.devops.jira.admin
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters

import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.protocol.HttpContext
import org.codehaus.groovy.runtime.StackTraceUtils
import static groovy.json.JsonOutput.toJson

import com.devops.jira.JIRARestAPI
import com.devops.urbancode.deploy.DeployRestAPI
import com.devops.utils.ConfigFile
/*
 * Create, Delete, Update(Start/Complete) Sprints
*/

@Slf4j
class SprintManager {
    
    HTTPBuilder httpBuilder
    JIRARestAPI jiraRestAPI
    
    SprintManager() {
        jiraRestAPI =  new JIRARestAPI()
        httpBuilder = jiraRestAPI.getHTTPBuilder()        
    }
    
	/*
	 * Read CSV file and return a list 
	 */
	List readCSV(String csvFile) {
		def fh = new File(csvFile)
	}
	
    /*
     * Can create multiple Sprints with the same name, internal id is different
     */
    def createSprint(String name, String originBoardId) {
        log.info("Create Sprint: name=${name}")
        
        httpBuilder.request(Method.POST) { req ->
            uri.path = '/rest/agile/latest/sprint'
            body = [name:name,originBoardId:originBoardId]
            requestContentType = ContentType.JSON
                     
            response.success = { resp, reader ->
                log.info("Succeeded: Sprint ${name} is created")
            }            
            response.failure = { resp, reader ->
                def text = reader.text    
                log.error("Failed: status ${resp.status} ${resp.statusLine}")
                log.error("        $text")              
            }
        }
    }
    
    /*
     * Can NOT be used to Start the Sprint
     * 
     */
    def pUpdateSprint(String id, String state, String value) {
        log.info("Partial Update Sprint to: ${state}:${value}")
        
        httpBuilder.request(Method.POST) { req ->
            uri.path = "/rest/agile/latest/sprint/${id}"
            body = [state:value] // the first one doesn't change
            requestContentType = ContentType.JSON
            
            response.success = { resp, reader ->
                log.info("Succeeded: Sprint ${state} is updated to ${value}")
            }
            response.failure = { resp, reader ->
                log.error("Failed: status ${resp.status} ${resp.statusLine}")

            }
        }
    }
    
	def completeSprint(String id) {
		pUpdateSprint(id, "state", "closed")
	}
	
	
    /*
     * Start the Sprint requires dates
     *
     */
    def startSprint(String id, String startDate, String endDate) {
        log.info("Start Sprint: ${id}")
        
        httpBuilder.request(Method.POST) { req ->
            uri.path = "/rest/agile/latest/sprint/${id}"
            body = [id:id,startDate:startDate,endDate:endDate,state:"active"]
            requestContentType = ContentType.JSON
            
            response.success = { resp, reader ->
                log.info("Succeeded: Sprint started")
            }
            response.failure = { resp, reader ->
                log.error("Failed: status ${resp.status} ${resp.statusLine}")

            }
        }
    }
    
    /*
     * Can create multiple Sprints with the same name, internal id is different
     */
    def deleteSprint(String id) {
        log.info("Delete Sprint: id=${id}")
        
        httpBuilder.request(Method.DELETE) { req ->
            uri.path = "/rest/agile/latest/sprint/${id}"
            headers."Accept" = 'application/json'
          
            response.success = { resp, reader ->
                log.info("Succeeded: Sprint ${id} is deleted")
            }
            response.failure = { resp, reader ->
                log.error("Failed: status ${resp.status} ${resp.statusLine}")
            }
        }
    }
    
    public static void main(String[] args) {
        SprintManager sm = new SprintManager()
        //JCommander jcmd = new JCommander(sm)
        def name = "Lab1 Sprint 1"
        def originBoardId = "65"
        def id = "4"
        def key = "state"
        def value = "active"
        def startDate = "2018-05-08"
        def endDate = "2018-05-18"
        
        try {
            //jcmd.parse(args)
            //sm.createSprint(name, originBoardId)
            //sm.deleteSprint(id)
            //sm.startSprint(id, startDate, endDate)
			sm.completeSprint(id)
            
        } catch (ParameterException ex) {
            System.out.println(ex.getMessage())
            //jcmd.usage()
        } catch (Throwable ta) {
            ta = StackTraceUtils.sanitizeRootCause(ta)
            log.error("FAILED", ta)            
        }
        
    }
}