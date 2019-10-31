#!/usr/bin/ksh

QAttr=`echo $1 | tr '[:lower:]' '[:upper:]' `        #Queue Attribute GET or PUT
QAttrValue=`echo $2 | tr '[:lower:]' '[:upper:]' `   #Queue Attribute Value
QMGR=$3         #Queue manager name
QName=$4        #Queue Name
QID=$5          #Queue Admin Service ID

prog=$(basename $0)

if [ "$#" -ne 7 ]; then
   echo "Syntax: $prog QueueAttribute QueueAttributeValue QueueManager Queue UserID"
   exit 1
fi

if [ $QAttr != 'GET' ] && [ $QAttr != 'PUT' ]; then
   echo "Invalid Queue Attribute. Valid Value is GET or PUT"
   exit 1
fi

if [ $QAttrValue != 'ENABLED' ] && [  $QAttrValue != 'DISABLED' ]; then
   echo "Invalid Queue Attribute Value. Valid value is ENABLED or DISABLED"
   exit 1
fi

MQTEMPFILE=`mktemp`|| { echo 1; exit 1; }

dmpmqaut -m $QMGR -t queue -n $QName -g $QID > $MQTEMPFILE 2>&1
grep 'authority:' $MQTEMPFILE | grep 'chg'  > /dev/null

if [ "$?" -ne 0 ]; then
   echo " MQRC_NOT_AUTHORIZED : Permission denied"
   exit 1
fi

QCHG=`echo "ALTER QLOCAL($QName) $QAttr($QAttrValue)" | /opt/mqm/bin/runmqsc $QMGR > $MQTEMPFILE 2>&1` 

if [ "$?" -ne 0 ]; then
   echo " CHANGE_FAILED : Please follow up with MQ Support"
   exit 1
else
   rm -rf $MQTEMPFILE || { echo 1; exit 1; }
fi

