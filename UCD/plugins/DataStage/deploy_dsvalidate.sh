#!/bin/ksh
set -u

prog=$(basename $0)

if [[ $# != 2 ]]; then
  echo "Syntax: $prog PROJECT_NAME JOB_NAME"
  exit 1
fi

ProjName=$1
JobName=$2

dsjob -run -mode VALIDATE -wait "$ProjName" "$JobName" 2>>/dev/null
DSReturn=$?
if [ "$DSReturn" = "0" ]; then
    jresult=`dsjob -jobinfo "$ProjName" "$JobName" 2>>/dev/null`
    if [ $? -ne 0 ]; then
        echo "ERROR: Unable to get job information for job $ProjName $JobName" 
        exit 1
    fi
    jstatus=`echo $jresult | head -1 | cut -d"(" -f2 | cut -d")" -f1`
    if [ "$jstatus" = "11" ]; then
       echo "SUCCESS: Validated OK job $ProjName $JobName" 
       exit 0
    else
       echo "ERROR: Validated FAILED job $ProjName $JobName" 
       if  [ "$jstatus" = "12" ] || [ "$jstatus" = "8" ] || [ "$jstatus" = "13" ]; then
            dsjob -logsum -type WARNING -max 1 "$ProjName" "$JobName" 2>>/dev/null   
       fi
       exit 1
    fi
else
   echo "ERROR: Cannot validate job $ProjName $JobName!!!"
   exit 1
fi
