#!/usr/bin/env groovy

import com.urbancode.air.AirPluginTool;
import propertyHelper;
import com.aestasit.ssh.DefaultSsh

def apTool = new AirPluginTool(this.args[0], this.args[1]);
def props = apTool.getStepProperties();
final def dbRoot = "/opt/devops/db_backup/"

def ucEnvType = props['environmenttype'].trim();
def ucITAMTeamName = props['ITAMTeamName'].trim();
def ucSchemaName = props['SchemaName'].trim();

def keyFileFullPath = System.getProperty("user.home") + '/conf/SFTPDB.whitelist'

File keyFile = new File(keyFileFullPath)
def foundKey = 0
def line
def sftpServer
def sftpID
def sftpPWD
def sftpFolder
def expiryDate

keyFile.withReader { reader ->
    while ((line = reader.readLine())!=null) {
        def keyArray = line.tokenize("|")
        if ( ucEnvType == keyArray[0] && ucITAMTeamName == keyArray[1] && ucSchemaName == keyArray[2]) {
            expiryDate = Date.parse("YYYY-MM-DD",keyArray[7])
            if ( expiryDate > new Date()) {
                foundKey = 1
                sftpServer = keyArray[3]
                sftpFolder = keyArray[4]
                sftpID     = keyArray[5]
                sftpPWD    = keyArray[6]
            }   
        }
    }
}
def sftpFileList=''
def sftpFile=''

if ( foundKey == 1) {
    def srcDir = new File(dbRoot+ucITAMTeamName)
    def fCnt   = 0

    DefaultSsh.remoteSession {
        user = sftpID
        password = sftpPWD
        host = sftpServer
        connect()
        srcDir.eachDirRecurse() { dir ->  
            dir.eachFileMatch(~/${ucEnvType}.*(?i)${ucSchemaName}.*.bak/) { file ->  
                scp {   
                    from { localFile(file.getPath()) }
                    into { remoteDir(sftpFolder) }
                }
                sftpFile = file.getPath() 
                sftpFileList = sftpFileList + sftpFile.substring(dbRoot.length(),sftpFile.length()) + '\n'
                fCnt = fCnt + 1
            }  
        }  
    println "$fCnt file(s) transferred"
    apTool.setOutputProperty("sftpFileList", sftpFileList);
    apTool.storeOutputProperties(); 
    }
} else
{
    throw new RuntimeException("FAIL --- SFTP database backup file is not allowed!")
}

