#!/bin/bash

. ${WVCCI_INST}/bin/wvcci_env/wvcci-env-l2-idepix.sh

l1bPath=$1
l1bFile=$2
idepixL2Dir=$3
year=$4
month=$5
wvcciRootDir=$6
snapDir=$7

l1bBaseName=`basename $l1bFile .N1`
echo "l1bBaseName: $l1bBaseName"

task="wvcci-l2-idepix-meris"
jobname="${task}-meris-${year}-${month}-${l1bBaseName}"
command="./bin/${task}-snap.sh ${l1bPath} ${l1bBaseName} ${idepixL2Dir} ${year} ${month} ${wvcciRootDir} ${snapDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
