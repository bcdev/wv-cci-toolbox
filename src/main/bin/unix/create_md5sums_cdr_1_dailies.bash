#!/bin/bash
echo "Generate file with md5sum for each final CDR-1 daily product on Calvalus ..."

for year in {2000..2023}; do
  for month in {01..12}; do
    # CDR-1:
    # /calvalus/projects/wvcci/tcwv/phase2_cdr1_final/dailies/2023/01/ESACCI-WATERVAPOUR-L3S-TCWV-MERGED-005deg-20230116-fv4.2.nc

    dailypath=/calvalus/projects/wvcci/tcwv/phase2_cdr1_final/dailies/$year/$month
    for product in $(hdfs dfs -stat "%n"  $dailypath/ESACCI-WATERVAPOUR-L3*.nc)
    do
      echo "generating md5sum for $dailypath/$product ..."
      md5="$(md5sum "$dailypath/$product")"
      md5="${md5%% *}"   # removes the first space and everything after it
      mkdir -p ./md5sums_cdr1_dailies/$year/$month
      echo "${md5}" > ./md5sums_cdr1_dailies/$year/$month/md5sum_${product}.txt
    done
  done
done

