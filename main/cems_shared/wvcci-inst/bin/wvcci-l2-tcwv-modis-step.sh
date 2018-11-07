#!/bin/bash

. ${WVCCI_INST}/bin/wvcci_env/wvcci-env-l2-tcwv-modis.sh

echo "entered wvcci-l2-tcwv-modis-step..."
l1bPath=$1
l1bFile=$2
year=$3
month=$4
day=$5
wvcciRootDir=$6

task="wvcci-l2-tcwv-modis"
jobname="${task}-${year}-${month}-${day}-${l1bFile}"
command="./bin/${task}-bash.sh ${l1bPath} ${l1bFile} ${year} ${month} ${day} ${wvcciRootDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
