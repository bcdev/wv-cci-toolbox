#!/bin/bash
echo "Killing pending bjobs ..."
BJOBS_PENDING=`squeue -l -u odanne |grep PEND | awk -F" " '{print $1}'`
bkill $BJOBS_PENDING
echo "Pending squeue bjobs killed."

echo "Killing running bjobs ..."
BJOBS_RUNNING=`squeue -l -u odanne |grep RUNN | awk -F" " '{print $1}'`
bkill $BJOBS_RUNNING
echo "Running squeue bjobs killed."
