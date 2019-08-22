#!/bin/bash

#sensors="meris-ssmi"
sensors="meris-modis_terra-ssmi"
#sensors="modis_terra-ssmi"
#resolutions="005 05"
resolutions="005"
#resolutions="05"
for year in {2011..2011}; do
  for month in {01..06}; do
    numDaysInMonth=`cal $month $year | awk 'NF {DAYS = $NF}; END {print DAYS}'`
    for res in $resolutions; do
      find /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc/${res}/${year}/${month}/01 -maxdepth 1 -name '*.nc' -printf '%P\0' | \
          tar --null -C /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc/${res}/${year}/${month}/01 \
	      --files-from=- -cvf WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}.tar
      for day in `seq -w 02 ${numDaysInMonth}`; do
        find /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc/${res}/${year}/${month}/${day} -maxdepth 1 -name '*.nc' -printf '%P\0' | \
            tar --null -C /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc/${res}/${year}/${month}/${day} \
                --files-from=- -uvf WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}.tar
      done
      gzip WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}.tar
    done
  done
done
