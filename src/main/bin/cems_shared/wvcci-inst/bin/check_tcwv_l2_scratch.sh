# ./check_tcwv_l2_scratch.sh MODIS_AQUA 2017
# ./check_tcwv_l2_scratch.sh MODIS_TERRA 2016 03 10

sensor=$1
year=$2

echo "Checking TCWV L2 on JASMIN scratch backup, sensor $sensor, year $year ..."
startTime=`date`
echo "Start time: $startTime"

sensor_lower=`echo $sensor | tr '[:upper:]' '[:lower:]'`

if [ -z ${3} ]; then
  firstMonth=01
else
  firstMonth=${3}
fi

if [ -z ${4} ]; then
  lastMonth=12
else
  lastMonth=${4}
fi

echo "Sensor: ${sensor_lower}"
echo "Year: $year"

for month in $(eval echo "{$firstMonth..$lastMonth}")
do
  echo "Month: $month"
  numDays=`cal $month $year | xargs echo | awk '{print $NF}'`;
  echo "numDays: $numDays"
  for i in `seq -w 01 $numDays`; do
    #echo "ls -l /work/scratch-nopw2/odanne/Tcwv/$sensor/$year/$month/$i/M* |wc"
    ls -l /work/scratch-nopw2/odanne/Tcwv/$sensor/$year/$month/$i/M* |wc
  done
done

echo "Done."
endTime=`date`
echo "End time: $endTime"

