#!/bin/bash

#sensors="meris-ssmi"
#sensors="meris-modis_terra-ssmi"
sensors="modis_terra-ssmi"
resolutions="005 05"
#resolutions="005"
#resolutions="05"
for year in {2012..2012}; do
  for month in {04..12}; do
    numDaysInMonth=`cal $month $year | awk 'NF {DAYS = $NF}; END {print DAYS}'`
    for res in $resolutions; do
      for day in `seq -w 01 ${numDaysInMonth}`; do
      #for day in `seq -w 26 26`; do
        #echo "mkdir -p /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc_DATASET1/${res}/${year}/${month}/${day}"
	mkdir -p /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc_DATASET1/${res}/${year}/${month}/${day}
	#echo "tar xzvf WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}${month}.tar.gz WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}/${month}${day}.nc"
	tar xzvf WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}.tar.gz WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}-${day}.nc
	#echo "cp WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}-${day}.nc /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc_DATASET1/${res}/${year}/${month}/${day}"
	cp WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}-${day}.nc /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc_DATASET1/${res}/${year}/${month}/${day}
	#echo "rm WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}-${day}.nc"
	rm WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}-${day}.nc
      done
    done
  done
done
