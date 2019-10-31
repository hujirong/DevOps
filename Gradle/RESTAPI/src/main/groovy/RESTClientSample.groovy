import groovyx.net.http.RESTClient
import java.net.URL

//def data = new URL(http://www.vogella.com).text
// alternatively use Groovy JDK methods
def data = 'http://www.vogella.com'.toURL().text
println data	
	
def client = new RESTClient( 'http://www.acme-online.de/' )
try {
	def resp = client.get( path : 'en/rc-models/helicopter/zoopa-300' ) // ACME boomerang
} catch (all) {
	println "exception"
}
if (resp.status !=200) {
	println resp.getData()
} else {
	assert resp.status == 200  // HTTP response code; 404 means not found, etc.
	println resp.getData()
}