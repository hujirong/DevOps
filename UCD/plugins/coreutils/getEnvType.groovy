#!/usr/bin/env groovy

import com.urbancode.air.AirPluginTool;
import propertyHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.utils.URIBuilder

import com.urbancode.commons.httpcomponentsutil.HttpClientBuilder;
import groovy.json.JsonSlurper

def apTool = new AirPluginTool(this.args[0], this.args[1]);
def props = apTool.getStepProperties();

def ucEnv = props['ucEnv'];
def ucApp = props['ucApp'];
def username = apTool.getAuthTokenUsername();
def password = apTool.getAuthToken();
def uctype

def ucURI  = new URIBuilder(System.getenv("AH_WEB_URL"))
ucURI.setPath("/cli/environment/info")
ucURI.addParameter("environment", ucEnv)
ucURI.addParameter("application", ucApp)

HttpClientBuilder clientBuilder = new HttpClientBuilder();
clientBuilder.setUsername(username)
clientBuilder.setPassword(password)

// for SSL enabled servers, accept all certificates
clientBuilder.setTrustAllCerts(true); 
DefaultHttpClient client = clientBuilder.buildClient();

try {
    HttpGet request = new HttpGet(new URI(ucURI.build().toString()));

    HttpResponse resp = client.execute(request);
    BufferedReader br = new BufferedReader ( 
        new InputStreamReader(resp.getEntity().getContent()));

    String currentLine = new String();
    while ((currentLine = br.readLine()) != null){
        def slurper = new JsonSlurper().parseText(currentLine)
        def ucRoleLabel = slurper.extendedSecurity.teams.resourceRoleLabel.toString().replace("[","").replace("]","")
        if (ucRoleLabel == 'DEV Environment') {
	         uctype = 'DEV'
	      } else if (ucRoleLabel == 'QA Environment') {
	         uctype = 'QA'
	      } else if (ucRoleLabel == 'UAT Environment') {
	         uctype = 'UAT'
	      } else if (ucRoleLabel == 'PROD Environment') {
	         uctype = 'PROD'
	      } else (
              uctype = 'NA'
        )         
    }
    apTool.setOutputProperty("environment.type", uctype);
    apTool.storeOutputProperties();              
  } catch (Exception e) {
    e.printStackTrace();
    throw new RuntimeException("FAIL --- get environment type for application:" + ucApp + " environment:" + ucEnv);
  }
