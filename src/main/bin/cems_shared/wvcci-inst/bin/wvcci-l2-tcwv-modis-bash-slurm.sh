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
cloudMaskPath=$3      # cloud product e.g. MOD35_L2.A2008016.1540.006.2012066182519.hdf
sensor=$4
year=$5
month=$6
day=$7
wvcciRootDir=$8

# Exit already here if L1b product is not in daily mode: (note the syntax: no [] brackets!!)
if ! `python $WVCCI_INST/bin/check_if_modis_daily_product.py $l1bPath`; then
  echo "SKIP nightly product $l1bPath ..."
  exit 0
else
  echo "DAILY product: $l1bPath ..."
  echo "START processing - wallclock time is: `date`"
fi

tmpdir=$wvcciRootDir/tmp
#tmpdir=/work/scratch-nompiio/odanne/wvcci/tmp
mkdir -p $tmpdir

modisstem=${l1bName%.hdf}
hour=${l1bName:18:2}
minute=${l1bName:20:2}
date_in_seconds=$(($(date +%s -u -d "$year-01-01 $hour:$minute:00") + ( 1$doy - 1000 ) * 86400 - 86400))
month=$(date --date "@$date_in_seconds" +%m)
day=$(date --date "@$date_in_seconds" +%d)

acquisitionTime=$year$month$day

# replace 'L1b' by 'L1bEraInterim':
l1bDir=`dirname $l1bPath`
current="L1b"
replace="L1bEraInterim"
l1bEraInterimDir=${l1bDir/$current/$replace}
mkdir -p $l1bEraInterimDir

replace="Tcwv"
tcwvDir=${l1bDir/$current/$replace}
mkdir -p $tcwvDir

eramodis=${tmpdir}/${modisstem}_era-interim.nc
echo "eramodis: $eramodis"

###############################
# was PROCESS step on Calvalus:
###############################

auxroot=$wvcciRootDir/auxiliary
let day_before_in_seconds="date_in_seconds - 86400"
let day_after_in_seconds="date_in_seconds + 86400"
date_before=`date +%Y-%m-%d -u -d @$day_before_in_seconds`
date_after=`date +%Y-%m-%d -u -d @$day_after_in_seconds`

# extract geo information in SCRIP format for CDOs:

echo "$(date +%Y-%m-%dT%H:%M:%S -u) extracting geo information of $l1bName ..."

# distinguish beginning of month, end of month, or in between and create time stacks
# we have no CDO on the scientific VMs which provide SLURM (and vice versa), 20200701
# --> Own CDO had to be installed in /gws/nopw/j04/esacci_wv/software, together with netcdf4, hdf, zlib
MY_CDO=/gws/nopw/j04/esacci_wv/software/cdo/bin/cdo
if [ "$day" = "01" -a "$year$month" != "197901" ]
then
  echo "$(date +%Y-%m-%dT%H:%M:%S -u) merge era stack with previous month ..."
  eradaybefore=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/${date_before:0:4}/era-interim-t2m-mslp-tcwv-u10-v10-${date_before:0:4}-${date_before:5:2}.nc
  erathisday=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/$year/era-interim-t2m-mslp-tcwv-u10-v10-$year-$month.nc
  eratimestack=era-interim-t2m-mslp-tcwv-u10-v10-${date_before:0:4}-${date_before:5:2}-$year-$month.nc
  echo "START cdo mergetime - wallclock time is: `date`"
  ${MY_CDO} -b 32 mergetime $eradaybefore $erathisday $eratimestack
  echo "END cdo mergetime - wallclock time is: `date`"
elif [ "$day" = "31" -a "$year$month" != "201509" ]
then
  echo "$(date +%Y-%m-%dT%H:%M:%S -u) merge era stack with next month ..."
  erathisday=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/$year/era-interim-t2m-mslp-tcwv-u10-v10-$year-$month.nc
  eradayafter=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/${date_after:0:4}/era-interim-t2m-mslp-tcwv-u10-v10-${date_after:0:4}-${date_after:5:2}.nc
  eratimestack=era-interim-tcwv-u10-v10-$year-$month-${date_after:0:4}-${date_after:5:2}.nc
  echo "START cdo mergetime - wallclock time is: `date`"
  ${MY_CDO} -b 32 mergetime $erathisday $eradayafter $eratimestack
  echo "END cdo mergetime - wallclock time is: `date`"
else
  # the normal case: this is a monthly Era interim file with a 6-hour time interval (e.g. 124 entries for August) 
  eratimestack=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/$year/era-interim-t2m-mslp-tcwv-u10-v10-$year-$month.nc
fi

# interpolate temporally
echo "$(date +%Y-%m-%dT%H:%M:%S -u) interpolate temporally ..."
eramodistimeslice=$tmpdir/era-interim-t2m-mslp-tcwv-u10-v10-$year$month$day$hour$minute.nc
echo "START cdo mergetime - wallclock time is: `date`"
${MY_CDO} inttime,$year-$month-$day,$hour:$minute:01 $eratimestack $eramodistimeslice
echo "END cdo inttime - wallclock time is: `date`"

# merge/collocate L1b with EraModis time slice:
# (this replaces the former spatial interpolation with CDO, and saves the gereration of scrip intermediate product, as well as the Era band subset, 20200701)
l1bEraInterimMerge=$l1bEraInterimDir/${modisstem}_l1b-era-interim.nc
echo "START gpt Merge/Collocate - wallclock time is: `date`"
$SNAP_HOME/bin/gpt Collocate -q 1 -Smaster=$l1bPath -Sslave=$eramodistimeslice -PrenameMasterComponents=false -PrenameSlaveComponents=false -PresamplingType=BILINEAR_INTERPOLATION -f Netcdf4-BEAM -t $l1bEraInterimMerge
echo "END gpt Merge/Collocate - wallclock time is: `date`"

# remove EraModis time slice...
sleep 10
# test: do not remove, inspect
# rm -f $eramodistimeslice

## TCWV
if [ -f $l1bEraInterimMerge ]; then
    #auxdataPath=/gws/nopw/j04/esacci_wv/software/dot_snap/auxdata/wvcci
    auxdataPath=/home/users/odanne/.snap/auxdata/wvcci
    tcwv=$tcwvDir/${modisstem}_tcwv.nc

    # Add MOD35_L2 product as cloud product. 
    # We want to write TCWV with NetCDF4_WVCCI in order NOT to write lat,lon!
    echo "START gpt Tcwv - wallclock time is: `date`"
    if [ -f $cloudMaskPath ]; then
      echo "${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 1 -e -SsourceProduct=$l1bEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv"
      #echo "${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 4 -e -SsourceProduct=$l1bEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv"
      #echo "${SNAP_HOME}/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$l1bEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv"
      ${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 1 -e -SsourceProduct=$l1bEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv
      #${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 4 -e -SsourceProduct=$l1bEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv
      #${SNAP_HOME}/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$l1bEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv
    fi
    echo "END gpt Tcwv - wallclock time is: `date`"
    status=$?
    echo "Status: $status"
fi

# cleanup: remove l1b-erainterim merge product (TCWV input)
echo "START cleanup - wallclock time is: `date`"
sleep 10
if [ -f $l1bEraInterimMerge ]; then
  # Merge products are large. As examples, just keep L1bEraInterim of Jan 15th and Jul 15th of each year:
  if [ "$day" != "15" ] || ([ "$month" != "01" ] && [ "$month" != "07" ]); then
    echo "DELETING L1bEraInterim merge product : $l1bEraInterimMerge"
    rm -f $l1bEraInterimMerge
  fi
fi

status=$?
if [ $status = 0 ] && [ -e "$tcwv" ]
then
    echo "TCWV product created."
    echo "Status: $status"
else
    echo "TCWV product NOT successfully created (corrupt or MODIS L1b is not a Day product)."
    echo "Status: $status"
    if [ -e "$tcwv" ]
    then
      echo "rm -f $tcwv"
      rm -f $tcwv   # delete unwanted file
    fi
fi
echo "END cleanup - wallclock time is: `date`"

echo "FINISHED job wvcci-tcwv-l2-modis - wallclock time is: `date`"
	






