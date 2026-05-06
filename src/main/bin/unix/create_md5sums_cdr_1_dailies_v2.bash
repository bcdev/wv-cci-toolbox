#!/bin/bash
echo "Generate file with md5sum for each final CDR-1 daily product on Calvalus ..."

res=005deg

for year in {2000..2023}; do
  for month in {01..12}; do
    # CDR-1:
    # $(pwd)/md5sums_cdr1_dailies/2021/07/md5sum_ESACCI-WATERVAPOUR-L3S-TCWV-MERGED-005deg-20210721-fv4.2.nc.txt

    dailypath=$(pwd)/md5sums_cdr1_dailies/$year/$month
    for md5singlefilepath in $(ls $dailypath/md5sum_ESACCI-WATERVAPOUR-L3*-${res}-*.nc.txt)
    do
      md5singlefilename=$(basename $md5singlefilepath)
      product=${md5singlefilename//"md5sum_"/""}  # ESACCI-WATERVAPOUR-L3S-TCWV-MERGED-005deg-20230116-fv4.2.nc.txt
      product=${product//".txt"/""}  # ESACCI-WATERVAPOUR-L3S-TCWV-MERGED-005deg-20230116-fv4.2.nc
      md5=$(cat $md5singlefilepath)
      echo "$md5 $product"
      echo "$md5 $product" >> md5sum_ESACCI-WATERVAPOUR-L3S-TCWV-MERGED-daily-${res}-fv4.2.nc.txt
    done
  done
done

