#!/bin/bash

. ${WVCCI_INST}/bin/wvcci-env-slurm.sh   # this script shall now be used for everything!

echo "entered NEW wvcci-l2-tcwv-modis-terra-era5-step-slurm..."
l1bPath=$1
l1bFile=$2
cloudMaskPath=$3
era5Path=$4
sensor=$5
year=$6
month=$7
day=$8
hhmm=$9
wvcciRootDir=${10}

task="wvcci-l2-tcwv-modis-terra-era5"
jobname="${task}-${year}-${month}-${day}-${hhmm}"
command0="./bin/${task}-bash-slurm.sh"
command="${command0} ${l1bPath} ${l1bFile} ${cloudMaskPath} ${era5Path} ${sensor} ${year} ${month} ${day} ${wvcciRootDir}"

echo "jobname: $jobname"
echo "command0: $command0"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

if [ -z ${jobs} ]; then
    timelim=120
    memlim=16000
    echo "submit_job ${jobname} ${command} ${timelim} ${memlim}"
    submit_job ${jobname} "${command}" ${timelim} ${memlim}
fi
