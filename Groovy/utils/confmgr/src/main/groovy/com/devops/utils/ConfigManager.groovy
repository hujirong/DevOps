package com.devops.utils

import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils

/**
 *  
 */
@Slf4j
class ConfigManager {
    static String KEY_FILE = '.devops'
    private ConfigFile conf
    static final String UCD_USER = 'urbancode.deploy.user'
    static final String UCD_PASSWORD = 'urbancode.deploy.password'
    
    
    static ConfigFile loadConf() {
        // config and keyFile are located at $user.home/devops/conf

        // keyFile
        File confDir = new File(System.getProperty('user.home') +  '/devops/conf')
        File keyFile = new File(confDir, KEY_FILE)
            
        // load confFile
        File confFile = new File(confDir, 'devops.properties')
        if (!confFile.exists()) {
            throw new Exception("Conf file $confFile is not exist")
        }
        
        log.info("Load conf file from $confFile")
        return new ConfigFile(confFile, keyFile)
    }
    
    static def getCredential(ConfigFile conf, String userName) {
        def user
        def password
        
        if (userName?.trim()) {
            user = userName    
            // userName left side in the property file
            // user: right side in the property file
            password = conf.getConfig(user + '.password', true)
            user = conf.getConfig(userName)
        } else {
            user = conf.getConfig(ConfigManager.UCD_USER)
            password = conf.getConfig(ConfigManager.UCD_PASSWORD, true)
        }
        
        return [user: user, password: password]
    }

    static def getValue(ConfigFile conf, String keyName) {
        return conf.getConfig(keyName)        
    }
    
    ConfigManager() {
    }
    
    
    public static void main(String[] args) {
        ConfigManager cm = new ConfigManager()
        
        try {
            def conf, user, password
            user = "jirong.hu"
            conf = ConfigManager.loadConf()
            password = cm.getCredential(conf, user)
            print "$user $password\n"
            //System.exit(0)
        } catch (Throwable ta) {
            ta = StackTraceUtils.sanitizeRootCause(ta)
            log.error("ConfigManager FAILED", ta)
           // System.exit(-1)
        }
        
        //System.exit(0)
    }
}
