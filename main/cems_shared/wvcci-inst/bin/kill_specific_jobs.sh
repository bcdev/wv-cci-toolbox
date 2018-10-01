#!/bin/bash
KILLME=$1
echo "Killing specific jobs containing '$KILLME'..."
KILLJOBS=`ps -ef | grep odanne |grep $KILLME | awk -F" " '{print $2}'`
echo $KILLJOBS
kill $KILLJOBS
