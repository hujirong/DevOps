#!/usr/bin/env groovy

import com.urbancode.air.AirPluginTool;
import propertyHelper;

def apTool = new AirPluginTool(this.args[0], this.args[1]);
def props = apTool.getStepProperties();

def ucEnvType = props['environmenttype'].trim();
def ucITAMTeamName = props['ITAMTeamName'].trim();
def ucRFC = props['RFC'].trim();
def database = props['database']

def BackupFolder = ''
def DataTimeStamp = ''
def RFCNumber = ''
def today = new Date()

BackupFolder  = ucITAMTeamName + "/" + database 
DataTimeStamp = today.format("yyyyMMddHHmmss")
if (ucRFC.length() > 0 && ucRFC.isNumber()) {
    RFCNumber = "RFC" + ucRFC
} else {
    RFCNumber = "NORFC"
}    

apTool.setOutputProperty("BackupFolder", BackupFolder);
apTool.setOutputProperty("DataTimeStamp", DataTimeStamp);
apTool.setOutputProperty("RFCNumber", RFCNumber);
apTool.storeOutputProperties();
