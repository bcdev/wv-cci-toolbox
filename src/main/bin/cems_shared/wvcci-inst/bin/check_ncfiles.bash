#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters. Usage: check_ncfiles.bash <year> < month>"
    exit 1
fi

year=$1
month=$2

numDays=`cal $month $year | xargs echo | awk '{print $NF}'`;
echo "numDays: $numDays"

for j in `seq -w 01 $numDays`; do
  echo "day: $j"
  for i in `ls -d /gws/nopw/j04/esacci_wv/odanne/WvcciRoot/Tcwv/MODIS_TERRA/$year/$month/$j/*.nc`; do
    ncdump -k $i > muell.txt
    status=$?
    #echo "Status: $status"
    rm -f muell.txt
  done
done

