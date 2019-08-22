#!/bin/bash

# Script to zip 'dataset1_1' (updated dataset1) for transfer to BC ftp
# After zipping, move files to appripriate subfolder in '/home/olaf/wvcci/dataset1/zip/' and call 'dataset1_1_zip_for_ftp_dailies.sh'.
# O.Danne, August 2019

#sensors="meris-cmsaf_hoaps"
sensors="meris-modis_terra"
#sensors="meris-modis_terra-cmsaf_hoaps"
#sensors="modis_terra-cmsaf_hoaps"
resolutions="005 05"
#resolutions="005"
#resolutions="05"
for year in {2012..2012}; do
  for month in {01..03}; do
    numDaysInMonth=`cal $month $year | awk 'NF {DAYS = $NF}; END {print DAYS}'`
    for res in $resolutions; do
      find /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc_DATASET1_1/${res}/${year}/${month}/01 -maxdepth 1 -name '*.nc' -printf '%P\0' | \
          tar --null -C /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc_DATASET1_1/${res}/${year}/${month}/01 \
	      --files-from=- -cvf WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}.tar
      for day in `seq -w 02 ${numDaysInMonth}`; do
        find /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc_DATASET1_1/${res}/${year}/${month}/${day} -maxdepth 1 -name '*.nc' -printf '%P\0' | \
            tar --null -C /calvalus/projects/wvcci/tcwv/${sensors}/l3-daily-final-nc_DATASET1_1/${res}/${year}/${month}/${day} \
                --files-from=- -uvf WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}.tar
      done
      gzip WV_CCI_L3_tcwv_${sensors}_${res}deg_${year}-${month}.tar
    done
  done
done
