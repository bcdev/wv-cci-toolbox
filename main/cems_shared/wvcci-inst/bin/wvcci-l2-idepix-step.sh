#!/bin/bash

. ${WVCCI_INST}/bin/wvcci_env/wvcci-env-l2-idepix.sh

l1bPath=$1
landMaskPath=$2
l1bFile=$3
landMaskFile=$4
idepixL2Dir=$5
sensor=$6
year=$7
month=$8
wvcciRootDir=$9
snapDir=${10}

if [ "$sensor" == "MERIS" ]
then
    l1bBaseName=`basename $l1bFile .N1`
elif [ "$sensor" == "MODIS_TERRA" ]
then
    l1bBaseName=`basename $l1bFile .hdf`
else
    echo "Invalid sensor $sensor - no Idepix processing started."
fi
echo "l1bBaseName: $l1bBaseName"

task="wvcci-l2-idepix"
jobname="${task}-${sensor}-${year}-${month}-${l1bBaseName}"
command="./bin/${task}-snap.sh ${l1bPath} ${landMaskPath} ${l1bFile} ${idepixL2Dir} ${sensor} ${year} ${month} ${wvcciRootDir} ${snapDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
