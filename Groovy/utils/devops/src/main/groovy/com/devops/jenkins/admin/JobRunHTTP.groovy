package com.devops.jenkins.admin
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
//https://support.cloudbees.com/hc/en-us/articles/226852648-How-to-build-a-job-using-the-REST-API-and-Java-

@Slf4j
class JobRunHTTP {
	@Parameter(names = "-U", description = "Jenkins user", required = true)
	String user
	@Parameter(names = "-P", description = "Jenkins password", required = true)
	String password
	@Parameter(names = "-URL", description = "Jenkins job URL", required = true)
	String url

	public void run() {
		try {
			log.info("URL="+url)
			URL url = new URL(url); // Jenkins URL localhost:8080, job named 'test'			
			//String user = user; // username
			//String pass = password; // password or API token
			String authStr = user + ":" + password;
			log.info(authStr)
			String encoding = Base64.getEncoder().encodeToString(authStr.getBytes("utf-8"));

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Authorization", "Basic " + encoding);
			InputStream content = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(content));
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
