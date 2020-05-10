#!/bin/bash

#. ${WVCCI_INST}/bin/wvcci_env/wvcci-env-l2-tcwv-modis.sh
. ${WVCCI_INST}/bin/wvcci-env.sh   # this script shall now be used for everything!

echo "entered NEW wvcci-l2-tcwv-modis-step..."
l1bPath=$1
l1bFile=$2
cloudMaskPath=$3
year=$4
month=$5
day=$6
hhmm=$7
wvcciRootDir=$8

task="wvcci-l2-tcwv-modis"
jobname="${task}-${year}-${month}-${day}-${hhmm}"
command0="./bin/${task}-bash_4.sh"
command="${command0} ${l1bPath} ${l1bFile} ${cloudMaskPath} ${year} ${month} ${day} ${wvcciRootDir}"

echo "jobname: $jobname"
echo "command0: $command0"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    #timelim=180
    #memlim=16000
    timelim=120
    memlim=16000
    echo "submit_job ${jobname} ${command} ${timelim} ${memlim}"
    submit_job ${jobname} "${command}" ${timelim} ${memlim}
fi
