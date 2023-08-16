#!/bin/bash
set -x
set -e

####################################################################
# Script for TCWV generation with MODIS L1b using SLURM.
# Basically follows original scripts 
# - era-interim-<sensor>-prepare
# - era-interim-<sensor>-process
# built on Calvalus by MB

####################################################################


###############################
# was PREPARE step on Calvalus: 
###############################

l1bPath=$1
l1bName=$2            # MOD021KM.A2008016.1540.006.2012066182519.hdf
cloudMaskPath=$3      # path to cloud product, e.g. MOD35_L2.A2008016.1540.006.2012066182519.hdf
era5Path=$4           # path to ERA5 product, e.g. era5_20220801_060000.nc
sensor=$5
year=$6
month=$7
day=$8
wvcciRootDir=$9

modisstem=${l1bName%.hdf}
hour=${l1bName:18:2}
minute=${l1bName:20:2}
date_in_seconds=$(($(date +%s -u -d "$year-01-01 $hour:$minute:00") + ( 1$doy - 1000 ) * 86400 - 86400))
month=$(date --date "@$date_in_seconds" +%m)
day=$(date --date "@$date_in_seconds" +%d)
acquisitionTime=$year$month$day

# replace 'L1b' by 'L1bEra5':
l1bDir=`dirname $l1bPath`
current="L1b"
replace="L1bEra5"
l1bEra5Dir=${l1bDir/$current/$replace}

replace="Tcwv"
tcwvDir=${l1bDir/$current/$replace}
mkdir -p $tcwvDir
tcwv=$tcwvDir/${modisstem}_tcwv.nc

# Exit already here if L1b product is not in daily mode: (note the syntax: no [] brackets!!)
if ! `python $WVCCI_INST/bin/check_if_modis_daily_product.py $l1bPath`; then
  echo "SKIP nightly product $l1bPath. Just write a dummy marker as TCWV 'result' ..."
  touch ${tcwv}.NIGHTLY
  exit 0
else
  echo "DAILY product: $l1bPath ..."
  echo "START processing - wallclock time is: `date`"
fi

tmpdir=$wvcciRootDir/tmp
#tmpdir=/work/scratch-nompiio/odanne/wvcci/tmp
mkdir -p $tmpdir
mkdir -p $l1bEra5Dir

eramodis=${tmpdir}/${modisstem}_era5.nc
echo "eramodis: $eramodis"

###############################
# was PROCESS step on Calvalus:
###############################

auxroot=$wvcciRootDir/auxiliary
let day_before_in_seconds="date_in_seconds - 86400"
let day_after_in_seconds="date_in_seconds + 86400"
date_before=`date +%Y-%m-%d -u -d @$day_before_in_seconds`
date_after=`date +%Y-%m-%d -u -d @$day_after_in_seconds`

# merge/collocate L1b with Era5:
l1bEra5Merge=$l1bEra5Dir/${modisstem}_l1b-era5.nc
if [ -f $era5Path ]; then
    echo "START gpt Merge/Collocate - wallclock time is: `date`"
    $SNAP_HOME/bin/gpt Collocate -q 1 -Smaster=$l1bPath -Sslave=$era5Path -PrenameMasterComponents=false -PrenameSlaveComponents=false -PresamplingType=BILINEAR_INTERPOLATION -f Netcdf4-BEAM -t $l1bEra5Merge
    echo "END gpt Merge/Collocate - wallclock time is: `date`"
else
    echo "ERA5 auxdata $era5Path does not exist - cannot generate reliable TCWV product."    
    exit -1
fi


## TCWV
if [ -f $l1bEra5Merge ] && [ -f $l1bEra5Merge ]; then
    auxdataPath=/gws/nopw/j04/esacci_wv/software/dot_snap/auxdata/wvcci
    #auxdataPath=/home/users/odanne/.snap/auxdata/wvcci
    tcwv=$tcwvDir/${modisstem}_tcwv.nc

    # Add MOD35_L2 product as cloud product. 
    # We want to write TCWV with NetCDF4_WVCCI in order NOT to write lat,lon!
    echo "START gpt Tcwv - wallclock time is: `date`"
    if [ -f $cloudMaskPath ]; then
      echo "${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 1 -e -SsourceProduct=$l1bEra5Merge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv"
      ${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 1 -e -SsourceProduct=$l1bEra5Merge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv
    fi
    echo "END gpt Tcwv - wallclock time is: `date`"
    status=$?
    echo "Status: $status"
fi

# cleanup: remove l1b-era5 merge product (TCWV input)
echo "START cleanup - wallclock time is: `date`"
sleep 10
if [ -f $l1bEra5Merge ]; then
  # Merge products are large. As examples, just keep L1bEra5 of Jan 15th and Jul 15th of each year:
  if [ "$day" != "15" ] || ([ "$month" != "01" ] && [ "$month" != "07" ]); then
    echo "DELETING L1bEra5 merge product : $l1bEra5Merge"
    rm -f $l1bEra5Merge
  fi
fi

status=$?
if [ $status = 0 ] && [ -e "$tcwv" ]
then
    echo "TCWV product created."
    echo "Status: $status"
else
    echo "TCWV product NOT successfully created (corrupt or missing input, or MODIS L1b is not a Day product)."
    echo "Status: $status"
    if [ -e "$tcwv" ]
    then
      echo "rm -f $tcwv"
      rm -f $tcwv   # delete unwanted file
    fi
fi
echo "END cleanup - wallclock time is: `date`"

echo "FINISHED job wvcci-tcwv-l2-modis-era5 - wallclock time is: `date`"
	






