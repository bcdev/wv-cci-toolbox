#!/bin/bash
echo "Generate file with md5sum for each final CDR-1 monthly product on Calvalus ..."

res=05deg

for year in {2000..2023}; do
    # CDR-1:
    # $(pwd)/md5sums_cdr1_monthlies/2021/md5sum_ESACCI-WATERVAPOUR-L3S-TCWV-MERGED-005deg-202107-fv4.2.nc.txt

    monthlypath=$(pwd)/md5sums_cdr1_monthlies/$year
    for md5singlefilepath in $(ls $monthlypath/md5sum_ESACCI-WATERVAPOUR-L3*-${res}-*.nc.txt)
    do
      md5singlefilename=$(basename $md5singlefilepath)
      product=${md5singlefilename//"md5sum_"/""}  # ESACCI-WATERVAPOUR-L3S-TCWV-MERGED-005deg-202301-fv4.2.nc.txt
      product=${product//".txt"/""}  # ESACCI-WATERVAPOUR-L3S-TCWV-MERGED-005deg-202301-fv4.2.nc
      md5=$(cat $md5singlefilepath)
      echo "$md5 $product"
      echo "$md5 $product" >> md5sum_ESACCI-WATERVAPOUR-L3-TCWV-monthly-${res}-fv4.2.nc.txt
    done
done

