#!/bin/bash

sensor=$1
year=$2
month=$3
res=$4

numDays=`cal $month $year | xargs echo | awk '{print $NF}'`;
echo "numDays: $numDays" 

for j in `seq -w 01 $numDays`; do
  echo "day: $j"
  for i in `ls -d /calvalus/projects/wvcci/tcwv/$sensor/l3-daily-final-nc/$res/$year/$month/$j/*.nc`; do 
    ncdump -k $i > muell.txt
    status=$?
    #echo "Status: $status"
    rm -f muell.txt
  done
done
