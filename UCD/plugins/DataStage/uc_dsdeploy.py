'''
Deploy DataStage Asset Automatically

Jim Liu
2015-03-10
'''

import sys
import optparse
import os
import time
import datetime
import lxml
from lxml import etree
import subprocess

class DSDeploy():
    def __init__(self, deploy_file, project_root, project_path, project_name, ds_domain, ds_host, istoolprog, dsuser, dspwd):
        self.deploy_file = deploy_file       
		self.project_root = project_root
		self.project_path = project_path
		self.project_name = project_name
		self.ds_domain = ds_domain
		self.ds_host = ds_host
		self.istoolprog = istoolprog
		self.dsuser = dsuser
		self.dspwd = dspwd
        self.strDateTime = datetime.datetime.now().strftime("%Y%b%d%I%M%S")
		self.progpath = os.path.dirname(os.path.realpath(__file__))
                    
        # Validate deploy xml file     
        schema_file = self.progpath + '/dsbuild.xsd'        
        with open(schema_file) as f_schema:
            schema_doc = etree.parse(f_schema)
            schema = etree.XMLSchema(schema_doc)
            parser = etree.XMLParser(schema = schema)

        with open(self.deploy_file) as f_source:
            try:
                doc  = etree.parse(f_source, parser)
                elem = doc.getroot()
                self.backuptarget   = elem.attrib.get("BACKUPTARGET")
                if self.backuptarget is None  :
                    self.backuptarget = 'N'  
                      
                self.changetargetconfig = elem.attrib.get("CHANGETARGETCONFIG")
                if self.changetargetconfig is None  :
                    self.changetargetconfig = 'N'  
                  
                self.packagelst = elem.find("PACKAGES")                  
				self.scripts = elem.find("SCRIPTS")
                self.validatejobs = elem.find("VALIDATEJOBS")     
                self.backupjobs = elem.find("BACKUPJOBS")              

            except etree.XMLSyntaxError as e:
                # this exception is thrown on schema validation error                  
                print('Invalid deployment file!!!')
                print e
                sys.exit(1)

        if self.backuptarget == 'C' or self.backuptarget == 'c' :
            vBackupLst = self.backupjobs.findall("BACKUPJOB")
            if len(vBackupLst) < 1 or len(vBackupLst) > 30 :
                print('Invalid Backup items defined in deployment file, a valid backup item range is 1-30!!!')
                sys.exit(1)
                
        print "Successfully validated deployment file %s" % self.deploy_file
	sys.stdout.flush()

    # deploy the application
    def deploy(self): 
        # Run Backup Script to backup related DataStage Project 
	strBackupFolder = self.project_root + '/archive/'  
	strBackupPkg    = self.project_name + '_' + self.strDateTime + '.isx'
	strBackupFile   = self.strDateTime + '_' + self.project_name  + '.zip'

	if  self.backuptarget == 'Y' or self.backuptarget == 'y' :
            print "Start backup DataStage project %s, it will take a while depending on the size of the project..." % self.project_name
	    sys.stdout.flush()
	    strBackupenv    = self.project_name + '_' + self.strDateTime + '.dspm'
	    strBackupScript = self.project_name + '_script_' + self.strDateTime + '.zip'

        strBackupCmd = self.istoolprog + ' export  -domain ' + self.ds_domain 
	    strBackupCmd = strBackupCmd + ' -username ' + self.dsuser + ' -password "' + self.dspwd + '" '
	    strBackupCmd = strBackupCmd + ' -ar ' + strBackupFolder + strBackupPkg + ' -ds ' + "'" +' -base="'
	    strBackupCmd = strBackupCmd + self.ds_host + '/' + self.project_name + '" Jobs/*/*.* -includedependent -incexec' + " ' -verbose "
	    
	    try:         
                p = subprocess.Popen(strBackupCmd, shell=True)
                stdout, stderr = p.communicate()
            except Exception, e:
                print e
		sys.stdout.flush()
                sys.exit(1)

	    if  p.returncode != 0:
	        print "Backup Project %s DataStage assets FAILED!!!" % self.project_name
		sys.stdout.flush()
	        sys.exit(1)
	    
            strBackupCmd = 'cp -p ' + self.project_path + '/DSParams ' + strBackupFolder + strBackupenv
            try:         
                p = subprocess.Popen(strBackupCmd, shell=True)
                stdout, stderr = p.communicate()
            except Exception, e:
                print e
		sys.stdout.flush()
                sys.exit(1)

	    if  p.returncode != 0:
	        print "Backup Project %s environment FAILED!!!" % self.project_name
		sys.stdout.flush()
	        sys.exit(1)
	
	    strBackupCmd = 'zip -r ' + strBackupFolder + strBackupScript + ' ' + self.project_root + '/scripts'
            try:         
                p = subprocess.Popen(strBackupCmd, shell=True)
                stdout, stderr = p.communicate()
            except Exception, e:
                print e
		sys.stdout.flush()
                sys.exit(1)

	    if  p.returncode != 0:
	        print "Backup Project %s scripts FAILED!!!" % self.project_name
		sys.stdout.flush()
	        sys.exit(1)

	    strBackupCmd = 'zip ' + strBackupFolder + strBackupFile + ' ' + strBackupFolder + strBackupPkg
	    strBackupCmd = strBackupCmd + ' ' + strBackupFolder + strBackupenv
            strBackupCmd = strBackupCmd + ' ' + strBackupFolder + strBackupScript

            try:         
                p = subprocess.Popen(strBackupCmd, shell=True)
                stdout, stderr = p.communicate()
            except Exception, e:
                print e
		sys.stdout.flush()
                sys.exit(1)

	    if  p.returncode != 0:
	        print "Zip all backup items of Project %s FAILED!!!" % self.project_name
		sys.stdout.flush()
	        sys.exit(1)

            strBackupCmd = 'rm -f ' + strBackupFolder + self.project_name + '*'
            try:         
                p = subprocess.Popen(strBackupCmd, shell=True)
                stdout, stderr = p.communicate()
            except Exception, e:
                print e
		sys.stdout.flush()
                sys.exit(1)

	    if  p.returncode != 0:
	        print "Clean up temp files of backup Project %s FAILED!!!" % self.project_name
		sys.stdout.flush()
	        sys.exit(1)

	    print "Backup Project %s is done successfully." % self.project_name
	    sys.stdout.flush()

	if  self.backuptarget == 'C' or self.backuptarget == 'c' :
            print "Start backup predefined DataStage Asset under project %s, it will take a while depending on the size of the project..." % self.project_name
	    sys.stdout.flush()
            vbackupLst = self.backupjobs.findall("BACKUPJOB")
            strBackupItems = ''
            for sitem in vbackupLst :          
                strBackupItems = strBackupItems + sitem.text + ' '
            strBackupItems = strBackupItems[:-1]

	    strBackupCmd = self.istoolprog + ' export  -domain ' + self.ds_domain 
	    strBackupCmd = strBackupCmd + ' -username ' + self.dsuser + ' -password "' + self.dspwd + '" '
	    strBackupCmd = strBackupCmd + ' -ar ' + strBackupFolder + strBackupPkg + ' -ds ' + "'" +' -base="'
	    strBackupCmd = strBackupCmd + self.ds_host + '/' + self.project_name + '/Jobs" '
	    strBackupCmd = strBackupCmd + strBackupItems + ' -incexec' + " ' -verbose 2>&1"

	    try:         
                p = subprocess.Popen(strBackupCmd, shell=True)
                stdout, stderr = p.communicate()
            except Exception, e:
                print e
		sys.stdout.flush()
                sys.exit(1)
	    
	    print stdout

	    if  p.returncode != 0:
	        print "Backup DataStage Project %s FAILED!!!" % self.project_name
		sys.stdout.flush()
	        sys.exit(1)

	    strBackupCmd = 'zip -m ' + strBackupFolder + strBackupFile + ' ' + strBackupFolder + strBackupPkg
	   
            try:         
                p = subprocess.Popen(strBackupCmd, shell=True)
                stdout, stderr = p.communicate()
            except Exception, e:
                print e
		sys.stdout.flush()
                sys.exit(1)

	    if  p.returncode != 0:
	        print "Zip all backup items of Project %s FAILED!!!" % self.project_name
		sys.stdout.flush()
	        sys.exit(1)

	    print "Backup Project %s is done successfully." % self.project_name
	    sys.stdout.flush()

	# Deploy package
        vPackageLst = self.packagelst.findall("PACKAGE")        
        if len(vPackageLst) > 0 :             
                
           for sitem in vPackageLst :  
               vPkgName = sitem.text
               vPkgBuild  = sitem.attrib.get("BUILDLABEL")
                    
               strDeployCmd = self.istoolprog + ' deploy package -domain ' + self.ds_domain 
	       strDeployCmd = strDeployCmd + ' -username ' + self.dsuser + ' -password "' + self.dspwd + '" '
	       strDeployCmd = strDeployCmd + ' -localfile ' + vPkgName + '.pkg -datastage "'
	       strDeployCmd = strDeployCmd + '-replace ' + self.ds_host + '/' + self.project_name + '"'
	       strDeployCmd = strDeployCmd + ' -lab "' + vPkgBuild + '" -verbose '

	       try:         
                    p = subprocess.Popen(strDeployCmd, shell=True)
                    stdout, stderr = p.communicate()
               except Exception, e:
                   print e
		   sys.stdout.flush()
                   sys.exit(1)

	       if  p.returncode != 0:
	           print "Deploy Package %s FAILED!!!" % vPkgName
		   sys.stdout.flush()
		   sys.exit(1)
	       else:
	           print "Package %s is successfully deployed." % vPkgName
		   sys.stdout.flush()

        # Deploy script tar file if needed
	if self.scripts is not None :
            scriptLst = self.scripts.findall("SCRIPT")
            if len(scriptLst) > 0 :
                print "Deploy script(s) ..."
		sys.stdout.flush()
	        strDeployCmd = 'tar -pxvf scripts.tar -C ' + self.project_root 
                try:         
                    p = subprocess.Popen(strDeployCmd, shell=True)
                    stdout, stderr = p.communicate()
                except Exception, e:
                    print e
		    sys.stdout.flush()
                    sys.exit(1)

	        if  p.returncode != 0:
	            print "Deploy script file(s) FAILED!!!" 
		    sys.stdout.flush()
	            sys.exit(1)
	        else:
	            print "Script files are successfully deployed." 
		    sys.stdout.flush()

	# Validate job if needed.
	if self.validatejobs is not None :
	    vjobLst = self.validatejobs.findall("VALIDATEJOB")
	    if len(vjobLst) > 0 :
	        strValidateCmd = self.progpath + '/deploy_dsvalidate.sh '
	        for sitem in vjobLst :                    
		    print "Validating job %s ..." % sitem.text
		    sys.stdout.flush()
                    strDeployCmd = strValidateCmd + ' ' + self.project_name + ' ' + sitem.text	        
                    try:         
                        p = subprocess.Popen(strDeployCmd, shell=True)
                        stdout, stderr = p.communicate()
                    except Exception, e:
                        print e
			sys.stdout.flush()
                        sys.exit(1)

	            if p.returncode != 0:
	                print "Validate job %s FAILED!!!" % sitem.text 
			sys.stdout.flush()
	                sys.exit(1)
	            else:
	                print "Job %s is successfully validated." % sitem.text  
			sys.stdout.flush()
 
        print "DataStage deployment of project %s is done successfully." % self.project_name
	sys.stdout.flush()

def main():
    usage  = 'usage: %prog [options] deploy_configurefile project_root project_path project_name ds_domain ds_host istoolprog dsuser dspwd'   
    parser = optparse.OptionParser(usage=usage)
    
    parser.add_option('-v', '--verbose', action='store_true', dest='verbose', default=False,
                      help='Display verbose message')
    
    (options, args) = parser.parse_args()

    if len(args) != 9:
        parser.print_help()
        sys.exit(1)

    ds_deploy = DSDeploy(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8])
    ds_deploy.deploy()

# entry point
if __name__ == '__main__':
    main()
