#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters. Usage: check_ncfiles.bash <year> < month>"
    exit 1
fi

year=$1
month=$2

numDays=`cal $month $year | xargs echo | awk '{print $NF}'`;
echo "numDays: $numDays"

tmpdir=/work/scratch-nompiio/odanne/wvcci/tmp
for j in `seq -w 01 $numDays`; do
  echo "day: $j"
  for i in `ls -d /gws/nopw/j04/esacci_wv/odanne/WvcciRoot/Tcwv/MODIS_AQUA/$year/$month/$j/*.nc`; do
    filename=`basename $i`
    stamp=$tmpdir/${filename}.stamp
    ncdump -k $i > $stamp
    #status=$?
    #echo "Status: $status"
    rm -f $stamp
  done
done

