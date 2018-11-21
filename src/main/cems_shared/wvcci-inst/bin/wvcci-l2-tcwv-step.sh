#!/bin/bash

. ${WVCCI_INST}/bin/wvcci_env/wvcci-env-l2-tcwv.sh

idepixPath=$1
idepixFile=$2
tcwvL2Dir=$3
sensor=$4
year=$5
month=$6
wvcciRootDir=$7
snapDir=$8

idepixBaseName=`basename $idepixFile .nc`
echo "idepixBaseName: $idepixBaseName"

task="wvcci-l2-tcwv"
jobname="${task}-${sensor}-${year}-${month}-${idepixBaseName}"
command="./bin/${task}-snap.sh ${idepixPath} ${idepixBaseName} ${tcwvL2Dir} ${sensor} ${year} ${month} ${wvcciRootDir} ${snapDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
