#!/bin/bash

sensor=$1
year=$2
month=$3
res=$4

numDays=`cal $month $year | xargs echo | awk '{print $NF}'`;
echo "sensor, resolution, year, month, numDays: $sensor, $res, $year, $month : $numDays" 

for j in `seq -w 01 $numDays`; do
  #echo "day: $j"
  for i in `ls -d /calvalus/projects/wvcci/tcwv/$sensor/l3-daily/$res/$year/$month/${j}-L3-1/*.nc`; do 
    ncdump -k $i > muell.txt
    status=$?
    #echo "Status: $status"
    rm -f muell.txt
  done
done
