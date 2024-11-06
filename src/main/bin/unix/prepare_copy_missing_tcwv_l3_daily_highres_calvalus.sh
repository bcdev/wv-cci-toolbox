#!/bin/bash
# ./prepare_copy_missing_tcwv_l3_daily_highres_calvalus.sh <firstYear> <lastYear>
# ./prepare_copy_missing_tcwv_l3_daily_highres_calvalus.sh 2017 2018
# ./prepare_copy_missing_tcwv_l3_daily_highres_calvalus.sh 2019 2022

#################################################################################
# This script finds gaps in the TCWV daily final highres 'best sensor combination' 
# products (input sensor(s) missing or not covering the ROI on that day)
# (modis_t-modia_a-olci_a for 2017, 2018 ; modis_t-modia_a-olci_a for 2019-2022).
# For an identified gap, a command is built which fills the gap with  the 'second best 
# sensor combination' product available.
# The commands are written sequentially in the form of a new bash script, therefore run the sequence:
#    - ./prepare_copy_missing_tcwv_l3_daily_highres_calvalus.sh <firstYear> <lastYear> > fill_gaps.sh
#    - chmod u+x fill_gaps.sh
#    - ./fill_gaps.sh 
#
# OD, 20241106
#################################################################################

firstYear=$1
lastYear=$2

version=fv4.2

echo "#!/bin/bash"
echo " "
echo "#FILL GAPS WITH CALVALUS TCWV SECOND BEST L3 DAILIES for HIGHRES in case of missing single sensors, years $firstYear - $lastYear ..."
echo " "

# declare sensor array
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
do
  roi="roi_$roi"

  for year in $(eval echo "{$firstYear..$lastYear}");
  do
    #echo $year
    for month in {06..09};
    do 
      numDays=`cal $month $year | xargs echo | awk '{print $NF}'`;
      for day in `seq -w 01 $numDays`; do
      #for day in `seq -w 01 03`; do
        # Check for all sensors. Print entry for given day if one or more sensors are missing.
        l3finalname="ESACCI-WATERVAPOUR-L3S-TCWV-"
        is_all_sensors=1
        all_sensors=""
        reduced_sensors=""
        for sensor in "${sensors[@]}"; do
          all_sensors=${all_sensors}-${sensor}
          # e.g. l3_tcwv_olci_a_001deg_roi_3_2018-08-15_2018-08-15.nc
          l3file=${tcwv_root}/$sensor/l3-daily-highres/001/$roi/$year/$month/$day/l3_tcwv_${sensor}_001deg_${roi}_${year}-${month}-${day}_${year}-${month}-${day}.nc
          if [ ! -f $l3file ]; then
            # No TCWV Daily L3 product available
            is_all_sensors=0
          else
            l3finalname=${l3finalname}_${sensor}
            reduced_sensors=${reduced_sensors}-${sensor}
          fi
        done
        all_sensors=${all_sensors:1}-cmsaf_hoaps
        reduced_sensors=${reduced_sensors:1}-cmsaf_hoaps

        # The L3 file name built from the available sensors.
        l3finalname=${l3finalname}_cmsaf_hoaps-001deg-${roi}-${year}${month}${day}-${version}.nc
        if [ ${is_all_sensors} -eq 0 ]; then
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

