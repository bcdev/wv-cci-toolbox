#!/bin/bash

# WVCCI function definitions

set -e

if [ -z "${WVCCI_INST}" ]; then
    WVCCI_INST=`pwd`
fi

WVCCI_TASKS=${WVCCI_INST}/tasks
WVCCI_LOG=${WVCCI_INST}/log
export PM_LOG_DIR=${WVCCI_LOG}
export PM_PYTHON_EXEC='/group_workspaces/cems2/qa4ecv/vol4/software/miniconda3/envs/wvcci/bin/python'

read_task_jobs() {
    echo "entered read_task_jobs()..."
    jobname=$1
    echo "jobname: $jobname"
    jobs=
    echo "WVCCI_TASKS/jobname.tasks: ${WVCCI_TASKS}/${jobname}.tasks"
    if [ -e ${WVCCI_TASKS}/${jobname}.tasks ]
    then
        for logandid in `cat ${WVCCI_TASKS}/${jobname}.tasks`
        do
	    echo "logandid: $logandid"
            job=`basename ${logandid}`
            log=`dirname ${logandid}`
	    echo "job: $job"
	    echo "log: $log"
            #if grep -qF 'Successfully completed.' ${log}
            #if ! grep -qF 'Status: 1' ${log}
            # also make sure that terminated jobs are not interpreted as successful:
            #if [ ! grep -qF 'Status: 1' ${log} ] && [ ! grep -qF 'TERM_RUNLIMIT' ${log} ]
            if [ grep -qF 'Status: 0' ${log} ] && [ ! grep -qF 'TERM_RUNLIMIT' ${log} ]
            then
                if [ "${jobs}" != "" ]
                then
                    jobs="${jobs}|${job}"
                else
                    jobs="${job}"
                fi
            fi
        done
    fi
}

submit_job() {
    jobname=$1
    command=$2
    echo "WVCCI_INST: ${WVCCI_INST}"
    echo "WVCCI_LOG : ${WVCCI_LOG}"
    echo "jobname: ${jobname}"
    echo "command: ${command}"

    # L2 TCWV MODIS:
    bsubmit="bsub -q short-serial -W 120 -R rusage[mem=16000] -M 16000 -P ga_qa4ecv -cwd ${WVCCI_INST} -oo ${WVCCI_LOG}/${jobname}.out -eo ${WVCCI_LOG}/${jobname}.err -J ${jobname} ${WVCCI_INST}/${command} ${@:3}"
    
    rm -f ${WVCCI_LOG}/${jobname}.out
    rm -f ${WVCCI_LOG}/${jobname}.err

    # line contains the console output of the bsub command
    line=`${bsubmit}`

    if echo ${line} | grep -qF 'is submitted'
    then
        # extract the job_id from the bsub message, concatenate '_' and jobname to form an identifier
        # and dump to std_out to be fetched by pmonitor
        job_id=`echo ${line} | awk '{ print substr($2,2,length($2)-2) }'`
        echo "${job_id}_${jobname}"
    else
        echo "`date -u +%Y%m%d-%H%M%S` - submit of ${jobname} failed: ${line}"
        exit 1
    fi

}
