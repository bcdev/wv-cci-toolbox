#!/bin/bash

test() {
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

    echo "jobname: ${jobname}"
    echo "command: ${command}"
    echo "timelim: ${timelim}"
    echo "memlim: ${memlim}"

    args=("$@")
    echo "$# arguments passed"
    echo ${args[0]} ${args[1]} ${args[2]}
    echo "eins: ${@:1}"    
    echo "zwei: ${@:2}"    
    echo "drei: ${@:3}"    
    echo "drei-drei: ${@:3:3}"    
    echo "drei-sechs: ${@:3:6}"    
    mynumargs=$(($#-2))
    echo "mynumargs: $mynumargs"
    echo "drei-last: ${@:3:${mynumargs}}"    

    # L2 TCWV MODIS:
    bsubmit_string="bsub -q short-serial ${timelim} ${memlim} -P ga_qa4ecv -cwd ${WVCCI_INST} -oo ${WVCCI_LOG}/${jobname}.out -eo ${WVCCI_LOG}/${jobname}.err -J ${jobname} ${WVCCI_INST}/${command} ${@:3:${mynumargs}}"
    echo "bsubmit_string: ${bsubmit_string}"    
}
