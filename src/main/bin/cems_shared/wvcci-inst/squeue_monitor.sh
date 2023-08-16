#!/bin/bash

while true
do
  RUNNING_ALL=`squeue |grep R |wc | awk -F" " '{print $1}'`
  PENDING_ALL=`squeue |grep PD |wc | awk -F" " '{print $1}'`
  RUNNING_ODANNE=`squeue -u odanne |grep R |wc | awk -F" " '{print $1}'`
  RUNNING_ODANNE=`expr ${RUNNING_ODANNE} - 1`
  PENDING_ODANNE=`squeue -u odanne |grep PD |wc | awk -F" " '{print $1}'`
  RUNNING_PERCENT_ODANNE=`echo "scale=1; 100 * ${RUNNING_ODANNE} / ${RUNNING_ALL}" |bc`
  echo "`date` : ${RUNNING_ALL} ${PENDING_ALL} ${RUNNING_ODANNE} ${PENDING_ODANNE} ${RUNNING_PERCENT_ODANNE}"

  sleep 60
done
