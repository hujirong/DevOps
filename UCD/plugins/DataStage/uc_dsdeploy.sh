#!/bin/bash
set -u

DSPATH=/opt/IBM/InformationServer/Server/DSEngine
. $DSPATH/dsenv;
PATH=/opt/IBM/InformationServer/Server/DSEngine/bin:$PATH;
export LD_PRELOAD=
DIRNAME=$(dirname $0)
export UCDS_HOME=$(readlink -e $DIRNAME)

# set PYTHON path
. /opt/common/cppib/bin/setEnvAnaconda.sh -c noCheck

DEPLOY_CONFIGUREBAK="ucdsdeploy.bak"
DEPLOY_CONFIGUREFILE="ucdsdeploy.xml"
PROJECT_NAME=$1
DS_DOMAIN=$2
DS_HOST=$HOSTNAME
DS_ISTOOLPROG="/opt/IBM/InformationServer/Clients/istools/cli/istool"
DS_USER=$3
DS_PWD=$4

dsadmin -listenv $PROJECT_NAME 2>&1 | grep RootFilePath > /dev/null
if [ "$?" -ne 0 ]; then
   echo "PROJECT_NOT_AUTHORIZED : Permission denied"
   exit 1
fi

PROJECT_ROOT=`dsadmin -listenv $PROJECT_NAME 2>&1 | egrep "^RootFilePath=" | cut -d'=' -f2`
ROOTSIZE=${#PROJECT_ROOT}
if [[ "$ROOTSIZE" -lt 20 ]]; then
   echo "INVALID_PROJECT"
   exit 1
fi 

PROJECT_PATH=`dsjob -projectinfo $PROJECT_NAME 2>&1 | grep 'Project Path' | cut -d: -f 2 | tr -d ' '`
if [ "$?" -ne 0 ]; then
   exit 1
fi

mv *.xml $DEPLOY_CONFIGUREBAK
if [ "$?" -ne 0 ]; then
   exit 1
fi

mv $DEPLOY_CONFIGUREBAK $DEPLOY_CONFIGUREFILE
if [ "$?" -ne 0 ]; then
   exit 1
fi

python $UCDS_HOME/uc_dsdeploy.py "$DEPLOY_CONFIGUREFILE" "$PROJECT_ROOT"  "$PROJECT_PATH" $PROJECT_NAME $DS_DOMAIN $DS_HOST $DS_ISTOOLPROG $DS_USER "$DS_PWD"
