#!/bin/bash

# check_l1b_neodc_completeness.sh MODIS_TERRA 2005 2010


if [[ $# -ne 3 ]]; then
    echo "Illegal number of parameters" >&2
    echo "Usage: check_l1b_neodc_completeness.sh <sensor> <firstYear> <lastYear>"
    exit 2
fi

sensor=$1
firstYear=$2
lastYear=$3

echo "Checking L1b completeness on NEODC, sensor $sensor"
startTime=`date`
echo "Start time: $startTime"

sensor_lower=`echo $sensor | tr '[:upper:]' '[:lower:]'`

echo "Sensor: ${sensor_lower}"

numFilesTotal=0
#for year in {2022..2022}
for year in $(eval echo "{$firstYear..$lastYear}")
do
  echo "Year: $year"
  numFilesInYear=0
  for month in {01..12}
  do
    numDays=`cal $month $year | xargs echo | awk '{print $NF}'`
    numFiles=`ls -l /gws/nopw/j04/esacci_wv/odanne/WvcciRoot/L1b/$sensor/$year/$month/*/M* |wc | awk -F" " '{print $1}'`
    numFilesExpected=`echo "scale=1; 288 * $numDays" |bc`
    if ((numFiles > numFilesExpected))
    then
      let numFiles=$((numFilesExpected))
    fi
    let numFilesInYear=$((numFilesInYear + numFiles))
    numFilesPercent=`echo "scale=1; 100 * ${numFiles} / ${numFilesExpected}" |bc`
    echo "$year$month : ${numFiles}/${numFilesExpected} --> $numFilesPercent pct"
  done

  if [ `expr $year % 4` -eq 0 ]
  then
    numDaysInYear=366    
  else
    numDaysInYear=365
  fi
  numFilesInYearExpected=`echo "scale=1; 288 * $numDaysInYear" |bc`
  numFilesPercent=`echo "scale=1; 100 * ${numFilesInYear} / ${numFilesInYearExpected}" |bc`
  echo "$year total : ${numFilesInYear}/${numFilesInYearExpected} --> $numFilesPercent pct"

  let numFilesTotal=$((numFilesTotal + numFilesInYear))
done

echo "Total number of $sensor L1b files in period ${firstYear}-${lastYear}: $numFilesTotal"
echo "Done."
endTime=`date`
echo "End time: $endTime"

