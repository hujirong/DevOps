package com.devops.versions;

import com.devops.Env

Env.APP_HOME = 'build/install/devops'

def uploader = new DataStageVersionUploader(new File('build/install/devops/conf/deployment-example.xml'))
uploader.uploadVersion("Dummy", "Dummy")