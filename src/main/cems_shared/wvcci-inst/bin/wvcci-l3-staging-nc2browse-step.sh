#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-staging-nc2browse.sh

echo "entered wvcci-l3-staging-ncbrowse-step..."
year=$1
month=$2
sensor=$3
stagingNc2browseFile=$4
stagingNc2browseResultDir=$5

task="wvcci-l3-staging-nc2browse"
jobname="${task}-${year}-${month}-${sensor}"
command="./bin/${task}-python.sh ${sensor} ${year} ${month} ${stagingNc2browseFile} ${stagingNc2browseResultDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
