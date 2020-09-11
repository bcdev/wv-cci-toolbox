#!/bin/bash

year=$1
month=$2

numDays=`cal $month $year | xargs echo | awk '{print $NF}'`;
echo "numDays: $numDays" 

for j in `seq -w 01 $numDays`; do
  echo "day: $j"
  for i in `ls -d /calvalus/projects/wvcci/tcwv/modis_terra/l2/$1/$2/$j/*.nc`; do 
    ncdump -k $i > muell.txt
    status=$?
    #echo "Status: $status"
    rm -f muell.txt
  done
done
