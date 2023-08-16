#!/bin/bash

while true
do
  USERS=( $(squeue -t RUNNING | awk -F" " '{print $4}') )

  declare -a USERS

  index=0
  num_users=1
  for user in ${USERS[@]};
  do
    if [ "$index" -gt 0 ]; then
      let indexm1=${index}-1
      #echo "$index -> ${USERS[index]} -> $indexm1 -> ${USERS[indexm1]}"
      if [ "${USERS[index]}" != "${USERS[indexm1]}" ]; then
        let num_users=${num_users}+1
        #echo "num_users: ${num_users}"
      fi
    fi
    let index=${index}+1
  done

  #echo "final num_users: ${num_users}"

  RUNNING_ALL=`squeue -t RUNNING |wc | awk -F" " '{print $1}'`
  PENDING_ALL=`squeue -t PENDING |wc | awk -F" " '{print $1}'`
  RUNNING_ODANNE=`squeue -u odanne -t RUNNING |wc | awk -F" " '{print $1}'`
  RUNNING_ODANNE=`expr ${RUNNING_ODANNE} - 1`
  PENDING_ODANNE=`squeue -u odanne -t PENDING |wc | awk -F" " '{print $1}'`
  RUNNING_PERCENT_ODANNE=`echo "scale=1; 100 * ${RUNNING_ODANNE} / ${RUNNING_ALL}" |bc`
  echo "`date` : ${num_users} ${RUNNING_ALL} ${PENDING_ALL} ${RUNNING_ODANNE} ${PENDING_ODANNE} ${RUNNING_PERCENT_ODANNE}"

  sleep 60
done
