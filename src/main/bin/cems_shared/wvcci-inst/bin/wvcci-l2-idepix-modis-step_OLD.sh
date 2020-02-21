#!/bin/bash

#. ${WVCCI_INST}/bin/wvcci_env/wvcci-env-l2-tcwv-modis.sh
. ${WVCCI_INST}/bin/wvcci-env.sh    # this script shall now be used for everything!

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

l1bBaseName=`basename $l1bFile .hdf`
echo "l1bBaseName: $l1bBaseName"

task="wvcci-l2-idepix-modis"
jobname="${task}-${sensor}-${year}-${month}-${l1bBaseName}"
command0="./bin/${task}-snap_OLD.sh"
command="${command0} ${l1bPath} ${landMaskPath} ${l1bFile} ${idepixL2Dir} ${sensor} ${year} ${month} ${wvcciRootDir} ${snapDir}"

echo "jobname: $jobname"
echo "command0: $command0"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    # use default parameters for time and memory limits in bsub call
    echo "submit_job ${jobname} ${command} 60 8000"
    submit_job ${jobname} "${command}" 60 8000
fi
