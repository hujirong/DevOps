#!/usr/bin/env groovy

import com.urbancode.air.AirPluginTool;
import propertyHelper;

def apTool = new AirPluginTool(this.args[0], this.args[1]);
def props = apTool.getStepProperties();

def ucEnvType = props['ucEnvType'].trim();
def ucServerInstance = props['ucServerInstance'].trim();
def ucDBName = props['ucDBName'].trim();
def ucITAMTeamName = props['ucITAMTeamName'].trim();
def ucRFC = props['ucRFC'].trim();

def BackupFolder = ''
def BackupFileName = ''
def today = new Date()

BackupFolder = ucITAMTeamName + "/MSSQL"
BackupFileName = ucEnvType + "_" + ucServerInstance.toString().replace("\\", "_") + "_" + ucDBName  + "_" + today.format("yyyyMMddHHmmss")
if (ucRFC.length() > 0) {
    BackupFileName = BackupFileName + "_RFC_" + ucRFC
}    
BackupFileName = BackupFileName + ".bak"

apTool.setOutputProperty("BackupFolder", BackupFolder);
apTool.setOutputProperty("BackupFileName", BackupFileName);
apTool.storeOutputProperties();
