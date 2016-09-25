package com.devops.utils

import org.apache.directory.groovyldap.LDAP
import org.apache.directory.groovyldap.SearchScope
import org.codehaus.groovy.runtime.StackTraceUtils
import groovy.util.logging.Slf4j

@Slf4j
class ADUtilities {
	String server = 'ldap.server'
	String user = 'ldap.binding.user'
	ConfigFile conf	
	def cred	
	LDAP connection
			
	ADUtilities() {
		conf = ConfigManager.loadConf()
		cred = ConfigManager.getCredential(conf, user)
		
		//connection = LDAP.newInstance('ldap://dcprod1.otpp.com:389','CN=Jirong Hu,OU=IT Operation,OU=Investment IT,OU=Office of the COO,OU=Toronto,OU=Investments,DC=otpp,DC=com', 'password')
		connection = LDAP.newInstance(ConfigManager.getValue(conf, server), cred.user, cred.password) 	//${cred.user}:${cred.password}
	}
	
	Boolean exist(String dn) {
		if ( dn == null) {
			log.error ("dn is null")		
		} 
		
		if (connection.exists(dn)) {
			//log.info "$dn exists\n"
			return true;
		} else {
			//log.info "$dn not exists\n"
			return false;
		}
	}
	
	public static void main(String[] args) {
		ADUtilities adu = new ADUtilities()	
		
		try {
			def person = 'CN=Jirong Hu,OU=IT Operation,OU=Investment IT,OU=Office of the COO,OU=Toronto,OU=Investments,DC=otpp,DC=com'
			//person = "CN=Bart Jasionowski,OU=OIM Deletion,DC=otpp,DC=com"
			person = 'CN=Bart Jasionowski,OU=IT Operation,OU=Investment IT,OU=Office of the COO,OU=Toronto,OU=Investments,DC=otpp,DC=com'
			assert (adu.connection.exists(person)):" Person $person does not exists"
			if (adu.exist(person)) {
				print "$person exists"
			} else {
				print "$person not exists"
			}
			
			System.exit(0)			
		} catch (Throwable ta) {
			ta = StackTraceUtils.sanitizeRootCause(ta)
			log.error("ADUtilities FAILED", ta)
			System.exit(-1)
		}
		
		System.exit(0)
	}
}

