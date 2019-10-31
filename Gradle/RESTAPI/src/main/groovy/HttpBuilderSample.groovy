import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.TEXT
import org.apache.http.protocol.HttpContext
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor

// initialze a new builder and give a default URL
def http = new HTTPBuilder( 'http://www.google.com/search' )

http.request(GET,TEXT) { req ->
	uri.path = '/mail/help/tasks/' // overrides any path in the default URL
	headers.'User-Agent' = 'Mozilla/5.0'

	response.success = { resp, reader ->
		assert resp.status == 200
		println "My response handler got response: ${resp.statusLine}"
		println "Response length: ${resp.headers.'Content-Length'}"
		System.out << reader // print response reader
	}

	// called only for a 404 (not found) status code:
	response.'404' = { resp -> println 'Not found' }

	http.handler.'401' = { resp -> println "Access denied" }

	// Used for all other failure codes not handled by a code-specific handler:
	http.handler.failure = { resp -> println "Unexpected failure: ${resp.statusLine}" }
}

HTTPBuilder getHTTPBuilder() {
	def httpBuilder = new HTTPBuilder(baseUrl)
	httpBuilder.ignoreSSLIssues()
	
	// don't use auth.basic because it sends request twice and for PUT request
	// it causes error:
	//   Cannot retry request with a non-repeatable request entity
	//
	// restClient.auth.basic(user, password)

	httpBuilder.client.addRequestInterceptor(new HttpRequestInterceptor() {
		void process(HttpRequest httpRequest, HttpContext httpContext) {
			httpRequest.addHeader('Authorization', 'Basic ' + "admin:admin123".bytes.encodeBase64().toString())
		}
	})
	
	// process error
	return httpBuilder
}