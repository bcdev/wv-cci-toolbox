# ./copy_era5_from_badc.sh 2023 02 05

year=$1
firstMonth=$2
lastMonth=$3

echo "Copy and merge ERA5 files with WVCCI variables from badc for year $year, months $firstMonth to $lastMonth ..."
startTime=`date`
echo "Start time: $startTime"

currDir=$(pwd)

badc_era5_root=/badc/ecmwf-era5/data/oper/an_sfc
gws_era5_root=/gws/nopw/j04/esacci_wv/odanne/WvcciRoot/auxiliary/era5_badc
era5_ftype=ecmwf-era5_oper_an_sfc

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

  for day in `seq -w 01 $numDaysInMonth`; do
    #echo "year: $year // month: $month // day: $day"
    mkdir -p ${gws_era5_root}/$year/$month/$day

    for hhmm in {0300..2100..0600}; do
      era5_src=${badc_era5_root}/$year/$month/$day/${era5_ftype}_$year$month$day$hhmm
      era5_target=${gws_era5_root}/$year/$month/$day/${era5_ftype}_$year$month$day$hhmm
      #echo "cdo merge ${era5_src}.10u.nc ${era5_src}.10v.nc ${era5_src}.2t.nc ${era5_src}.msl.nc ${era5_src}.tcwv.nc ${era5_target}.nc"
      echo "cdo merge --> ${era5_target}.nc"
      cdo merge ${era5_src}.10u.nc ${era5_src}.10v.nc ${era5_src}.2t.nc ${era5_src}.msl.nc ${era5_src}.tcwv.nc ${era5_target}.nc
    done
  done
done

echo "Done."
endTime=`date`
echo "End time: $endTime"

