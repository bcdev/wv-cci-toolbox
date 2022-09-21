#!/bin/bash

# WVCCI function definitions
# usage ${mms_home}/bin/${WVCCI_ENV_NAME}  (in xxx-start.sh and xxx-run.sh)

# project and user settings
# -------------------------
export PROJECT=wvcci

# Java and Python runtime definitions
# -----------------------------------

# ensure that processes exit
set -e

if [ -z "${WORKING_DIR}" ]; then
    WORKING_DIR=`pwd -P`
fi

export PM_LOG_DIR=${WORKING_DIR}/log

if [ "$SCHEDULER" == "LSF" ]; then

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
          memlim="-R rusage[mem=${memlim}] -M ${memlim}"
        fi

        bsubmit="bsub -q short-serial -We 2:00 -W 4:00 ${memlim} -P ${PROJECT} -cwd ${WVCCI_INST} -oo ${WVCCI_LOG}/${jobname}.out -eo ${WVCCI_LOG}/${jobname}.err -J ${jobname} ${WVCCI_INST}/${command} ${@:3:${mynumargs}}"

        rm -f ${PM_LOG_DIR}/${jobname}.out
        rm -f ${PM_LOG_DIR}/${jobname}.err

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

elif [ "$SCHEDULER" == "SLURM" ]; then

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
              memlim="--mem=${memlim}"
            fi

            command="${WVCCI_INST}/${command} ${@:3:${mynumargs}}"
            bsubmit="sbatch -p short-serial --time-min=01:00:00 --time=02:00:00 ${memlim} --chdir=${WVCCI_INST} -o ${WVCCI_LOG}/${jobname}.out -e ${WVCCI_LOG}/${jobname}.err --open-mode=append --job-name=${jobname} ${command}"
            echo "bsubmit: ${bsubmit}"  

            rm -f ${PM_LOG_DIR}/${jobname}.out
            rm -f ${PM_LOG_DIR}/${jobname}.err

            # line contains the console output of the bsub command
            line=`${bsubmit}`

            if echo ${line} | grep -qF 'Submitted batch job'
            then
                # extract the job_id from the bsub message, concatenate '_' and jobname to form an identifier
                # and dump to std_out to be fetched by pmonitor
                job_id=`echo ${line} | awk '{ print substr($4,0,length($4)) }'`
                echo "${job_id}_${jobname}"
            else
                echo "`date -u +%Y%m%d-%H%M%S` - submit of ${jobname} failed: ${line}"
                exit 1
            fi
        }

else
    echo "Invalid scheduler"
    exit 1
fi
