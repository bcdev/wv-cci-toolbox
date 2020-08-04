#!/bin/bash

# WVCCI function definitions

set -e

if [ -z "${WVCCI_INST}" ]; then
    WVCCI_INST=`pwd`
fi

WVCCI_TASKS=${WVCCI_INST}/tasks
WVCCI_LOG=${WVCCI_INST}/log
#export PM_LOG_DIR=${WVCCI_LOG}
#export PM_PYTHON_EXEC='/gws/nopw/j04/esacci_wv/software/miniconda3/envs/wvcci/bin/python'

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
    timelim=$3  # job time limit in minutes, see https://help.jasmin.ac.uk/article/113-submit-jobs, default: 60
    memlim=$4   # job memory limit in MB, see https://help.jasmin.ac.uk/article/113-submit-jobs, default: 4000

    if [ ! -z "$timelim" ];
    then
      timelim="-W ${timelim}"
    fi
    if [ ! -z "$memlim" ];
    then
      # LSF:
      #memlim="-R rusage[mem=${memlim}] -M ${memlim}"
      # SLURM:
      memlim="--mem=${memlim}"
    fi

    echo "WVCCI_INST: ${WVCCI_INST}"
    echo "WVCCI_LOG : ${WVCCI_LOG}"
    echo "jobname: ${jobname}"
    echo "command: ${command}"
    echo "timelim: ${timelim}"
    echo "memlim: ${memlim}"

    args=("$@")
    echo "$# arguments passed"
    numMoreArgs=$(($#-2))

    # L2 TCWV MODIS:
    # LSF:
    #bsubmit="bsub -q short-serial -We 2:00 -W 4:00 ${memlim} -P ga_qa4ecv -cwd ${WVCCI_INST} -oo ${WVCCI_LOG}/${jobname}.out -eo ${WVCCI_LOG}/${jobname}.err -J ${jobname} ${WVCCI_INST}/${command} ${@:3:${mynumargs}}"
    
    # SLURM:
    command_string="${WVCCI_INST}/${command} ${@:3:${mynumargs}}"
    bsubmit_string="sbatch -p short-serial --time-min=01:00:00 --time=02:00:00 ${memlim} --chdir=${WVCCI_INST} -o ${WVCCI_LOG}/${jobname}.out -e ${WVCCI_LOG}/${jobname}.err --open-mode=append --job-name=${jobname} --wrap=\"${command_string}\""
  
    echo "bsubmit: ${bsubmit_string}"
  
    rm -f ${WVCCI_LOG}/${jobname}.out
    rm -f ${WVCCI_LOG}/${jobname}.err

    # This invokes the SLURM job:
    bsubmit=`sbatch -p short-serial --time-min=02:00:00 --time=04:00:00 ${memlim} --chdir=${WVCCI_INST} -o ${WVCCI_LOG}/${jobname}.out -e ${WVCCI_LOG}/${jobname}.err --open-mode=append --job-name=${jobname} --wrap="${command_string}"`

    # line contains the console output of the bsub command
    #line=`${bsubmit}`
    line=${bsubmit_string}

    if echo ${line} | grep -qF 'is submitted'
    then
        # extract the job_id from the bsub message, concatenate '_' and jobname to form an identifier
        # and dump to std_out to be fetched by pmonitor
        job_id=`echo ${line} | awk '{ print substr($2,2,length($2)-2) }'`
        echo "${job_id}_${jobname}"

        # clarify: should look like in old setup?
        #jobs=`echo ${line} | awk '{ print substr($2,2,length($2)-2) }'`
        #echo "${WVCCI_LOG}/${jobname}.out/${jobs}" > ${WVCCI_TASKS}/${jobname}.tasks
        #echo "jobs: $jobs"
        # --> obviously this is the reason why 'tasks' dir was actually always empty (OD, 20191017)
    else
        echo "`date -u +%Y%m%d-%H%M%S` - submit of ${jobname} failed: ${line}"
        #echo "`date -u +%Y%m%d-%H%M%S` - tasks for ${jobname} failed. Reason: was not submitted."
        exit 1
    fi

}
