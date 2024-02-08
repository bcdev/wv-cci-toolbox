# ./download_mod35_l2_from_laads.sh 2005 02 05

year=$1
firstMonth=$2
lastMonth=$3

echo "Download MOD35_L2 from NASA LAADS for year $year, months $firstMonth to $lastMonth ..."
startTime=`date`
echo "Start time: $startTime"

#let num_users=${num_users}+1

currDir=$(pwd)

for month in $(eval echo "{$firstMonth..$lastMonth}")
do
  echo $month

  startDatestring=$(echo ${year}-${month}-01)
  startDayOfYear=$(date -d $startDatestring +%j)
  numDaysInMonth=`cal $month $year | xargs echo | awk '{print $NF}'`;
  endDatestring=$(echo ${year}-${month}-${numDaysInMonth})
  endDayOfYear=$(date -d $endDatestring +%j)

  echo "startDayOfYear: $startDayOfYear"
  echo "numDaysInMonth: $numDaysInMonth"
  echo "endDayOfYear: $endDayOfYear"

  for day in `seq -w $startDayOfYear $endDayOfYear`; do
    echo "day: $day"
    cd /gws/nopw/j04/esacci_wv/odanne/WvcciRoot/ModisCloudMask/MOD35_L2_LAADS/$year/$month
    #echo "wget -e robots=off -m -np -R .html,.tmp -nH --cut-dirs=5 https://ladsweb.modaps.eosdis.nasa.gov/archive/allData/61/MOD35_L2/2005/$day --header Authorization: Bearer -P $(pwd)"
    echo wget -e robots=off -m -np -R .html,.tmp -nH --no-verbose --cut-dirs=5 "https://ladsweb.modaps.eosdis.nasa.gov/archive/allData/61/MOD35_L2/2005/$day" --header "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBUFMgT0F1dGgyIEF1dGhlbnRpY2F0b3IiLCJpYXQiOjE2OTY0MTc4ODIsIm5iZiI6MTY5NjQxNzg4MiwiZXhwIjoxODU0MDk3ODgyLCJ1aWQiOiJvZGE2NiIsImVtYWlsX2FkZHJlc3MiOiJvZGE2NkB3ZWIuZGUiLCJ0b2tlbkNyZWF0b3IiOiJvZGE2NiJ9.4zfchefvf8MQMQl9vIGQAKWmU3cT_OpDleIytvxkf5w" -P $(pwd)
    wget -e robots=off -m -np -R .html,.tmp -nH --cut-dirs=7 "https://ladsweb.modaps.eosdis.nasa.gov/archive/allData/61/MOD35_L2/2005/$day" --header "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBUFMgT0F1dGgyIEF1dGhlbnRpY2F0b3IiLCJpYXQiOjE2OTY0MTc4ODIsIm5iZiI6MTY5NjQxNzg4MiwiZXhwIjoxODU0MDk3ODgyLCJ1aWQiOiJvZGE2NiIsImVtYWlsX2FkZHJlc3MiOiJvZGE2NkB3ZWIuZGUiLCJ0b2tlbkNyZWF0b3IiOiJvZGE2NiJ9.4zfchefvf8MQMQl9vIGQAKWmU3cT_OpDleIytvxkf5w" -P $(pwd)/$day
  done
done

cd $currDir

echo "Done."
endTime=`date`
echo "End time: $endTime"

