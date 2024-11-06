#!/bin/bash
# ./check_tcwv_missing_l3_daily_highres_calvalus.sh <firstYear> <lastYear>
# ./check_tcwv_missing_l3_daily_highres_calvalus.sh 2017 2018
# ./check_tcwv_missing_l3_daily_highres_calvalus.sh 2019 2022

firstYear=$1
lastYear=$2

version=fv4.2

echo "#!/bin/bash"
echo " "
echo "#FILL GAPS WITH CALVALUS TCWV SECOND BEST L3 DAILIES for HIGHRES in case of missing single sensors, years $firstYear - $lastYear ..."
echo " "

#startTime=`date`
#echo "Start time: $startTime"

# OLCI B starts in 2019...
firstYearInt=$(($firstYear + 0))
if [[ $firstYearInt -ge 2019 ]]; then
  declare -a sensors=("modis_terra" "modis_aqua" "olci_a"  "olci_b")
else
  declare -a sensors=("modis_terra" "modis_aqua" "olci_a")
fi

tcwv_root=/calvalus/projects/wvcci/tcwv

# LOOP over ROIs, years, month, days, and potentionally available sensors...
for roi in {1..3};
#for roi in {1..1};
do
  roi="roi_$roi"
  echo "#$roi"

  for year in $(eval echo "{$firstYear..$lastYear}");
  do
    #echo $year
    for month in {06..09};
    #for month in {06..06};
    do 
      #echo $month
      numDays=`cal $month $year | xargs echo | awk '{print $NF}'`;
      #echo "numDays: $numDays"
      for day in `seq -w 01 $numDays`; do
      #for day in `seq -w 01 03`; do
        # Check for all sensors. Print entry for given day if one or more sensors are missing.
        l3name="l3_tcwv_"
        l3finalname="ESACCI-WATERVAPOUR-L3S-TCWV-"
        missing=""
        is_all_sensors=1
        all_sensors=""
        reduced_sensors=""
        for sensor in "${sensors[@]}"; do
          all_sensors=${all_sensors}-${sensor}
          # e.g. l3_tcwv_olci_a_001deg_roi_3_2018-08-15_2018-08-15.nc
          l3file=${tcwv_root}/$sensor/l3-daily-highres/001/$roi/$year/$month/$day/l3_tcwv_${sensor}_001deg_${roi}_${year}-${month}-${day}_${year}-${month}-${day}.nc
          #echo "File: ${l3file}"
          if [ ! -f $l3file ]; then
            # No TCWV Daily L3 product available
            #echo "No TCWV Daily L3 product available for: sensor ${sensor} , ${roi} , $year-$month-${day}"
            missing="${missing} ${sensor}"
            is_all_sensors=0
          else
            l3name=${l3name}_${sensor}
            l3finalname=${l3finalname}_${sensor}
            reduced_sensors=${reduced_sensors}-${sensor}
          fi
        done
        all_sensors=${all_sensors:1}-cmsaf_hoaps
        _all_sensors=${all_sensors//-/_}
        reduced_sensors=${reduced_sensors:1}-cmsaf_hoaps
        _reduced_sensors=${reduced_sensors//-/_}

        # The L3 file name built from the available sensors.
        l3name=${l3name}_${roi}_${year}-${month}-${day}_${year}-${month}-${day}.nc
        l3finalname=${l3finalname}_cmsaf_hoaps
        l3finalname=${l3finalname}-001deg-${roi}-${year}${month}${day}-${version}.nc
        if [ ${is_all_sensors} -eq 0 ]; then
          #printf "%s   %s   %24s   %s\n" $roi   $year-$month-$day   $missing   $l3name
          #printf "%s   %s   %24s   %s\n" $roi   $year-$month-$day   $missing   ${l3finalname//-_/-}
          cp_cmd="cp ${tcwv_root}/${reduced_sensors}/l3-daily-final-highres-nc/001/${roi}/${year}/${month}/${day}/${l3finalname//-_/-} ${tcwv_root}/${all_sensors}/l3-daily-final-highres-nc/001/${roi}/${year}/${month}/${day}"
          echo "echo \"${cp_cmd}\""
          echo ${cp_cmd}
        fi
      done
    done
  done
done

echo " "
echo "#Done."
endTime=`date`
#echo "End time: $endTime"

