#!/bin/bash
echo "Generate file with md5sum for each final CDR-1 monthly product on Calvalus ..."

for year in {2000..2023}; do
    # CDR-1:
    # /calvalus/projects/wvcci/tcwv/phase2_cdr1_final/monthlies/2013/ESACCI-WATERVAPOUR-L3S-TCWV-MERGED-005deg-201308-fv4.2.nc

    monthlypath=/calvalus/projects/wvcci/tcwv/phase2_cdr1_final/monthlies/$year
    for product in $(hdfs dfs -stat "%n"  $monthlypath/ESACCI-WATERVAPOUR-L3*.nc)
    do
      echo "generating md5sum for $monthlypath/$product ..."
      md5="$(md5sum "$monthlypath/$product")"
      md5="${md5%% *}"   # removes the first space and everything after it
      mkdir -p ./md5sums_cdr1_monthlies/$year
      echo "${md5}" > ./md5sums_cdr1_monthlies/$year/md5sum_${product}.txt
    done
done

