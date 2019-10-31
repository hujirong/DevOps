#!/usr/bin/env groovy

import com.urbancode.air.AirPluginTool;
import propertyHelper;

def apTool = new AirPluginTool(this.args[0], this.args[1]);
def props = apTool.getStepProperties();

def ucEnvType = props['ucEnvType'].trim();
def ucConnectionID = props['ucConnectionID'].trim();
def ucSchemaName = props['ucSchemaName'].trim();
def ucITAMTeamName = props['ucITAMTeamName'].trim();
def ucRFC = props['ucRFC'].trim();

def BackupFolder = ''
def BackupFileName = ''
def today = new Date()

BackupFolder = ucITAMTeamName + "/ORACLE"

def scanString = ucConnectionID.substring(0, ucConnectionID.indexOf("cppib")-1)
def scanArray = scanString.tokenize(".:@/")
def scanName  = "_" + scanArray[scanArray.size() - 1 ]

BackupFileName = ucEnvType + scanName + "_" + ucSchemaName  + "_" + today.format("yyyyMMddHHmmss")
if (ucRFC.length() > 0) {
    BackupFileName = BackupFileName + "_RFC_" + ucRFC
}    
BackupFileName = BackupFileName + ".bak"

apTool.setOutputProperty("BackupFolder", BackupFolder);
apTool.setOutputProperty("BackupFileName", BackupFileName);
apTool.storeOutputProperties();
