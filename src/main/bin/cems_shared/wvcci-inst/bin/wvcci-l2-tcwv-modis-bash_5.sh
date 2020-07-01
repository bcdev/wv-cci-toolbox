#!/bin/bash
set -x
set -e

####################################################################
# Script for collocation of EraInterim auxdata product with MODIS L1b.
# Follows original scripts 
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
#if [ -f $eramodis ]; then
#  echo "EraInterim file '$eramodis' already exists - exit (code 0)."
#  exit 0
#fi


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

scripfile=$tmpdir/${modisstem}-scrip.nc
if [ ! -e $scripfile ]; then
  echo "START gpt Write SCRIP - wallclock time is: `date`"
  if ! $SNAP_HOME/bin/gpt Write -q 1 -PformatName=SCRIP -Pfile=$scripfile $l1bPath
  #if ! $SNAP_HOME/bin/gpt Write -q 4 -PformatName=SCRIP -Pfile=$scripfile $l1bPath
  #if ! $SNAP_HOME/bin/gpt Write -PformatName=SCRIP -Pfile=$scripfile $l1bPath
  then
    cat gpt.out
    exit 1
  fi
  echo "END gpt Write SCRIP - wallclock time is: `date`"

fi

# distinguish beginning of month, end of month, or in between and create time stacks

if [ "$day" = "01" -a "$year$month" != "197901" ]
then
  echo "$(date +%Y-%m-%dT%H:%M:%S -u) merge era stack with previous month ..."
  eradaybefore=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/${date_before:0:4}/era-interim-t2m-mslp-tcwv-u10-v10-${date_before:0:4}-${date_before:5:2}.nc
  erathisday=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/$year/era-interim-t2m-mslp-tcwv-u10-v10-$year-$month.nc
  eratimestack=era-interim-t2m-mslp-tcwv-u10-v10-${date_before:0:4}-${date_before:5:2}-$year-$month.nc
  echo "START cdo mergetime - wallclock time is: `date`"
  cdo -b 32 mergetime $eradaybefore $erathisday $eratimestack
  echo "END cdo mergetime - wallclock time is: `date`"
elif [ "$day" = "31" -a "$year$month" != "201509" ]
then
  echo "$(date +%Y-%m-%dT%H:%M:%S -u) merge era stack with next month ..."
  erathisday=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/$year/era-interim-t2m-mslp-tcwv-u10-v10-$year-$month.nc
  eradayafter=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/${date_after:0:4}/era-interim-t2m-mslp-tcwv-u10-v10-${date_after:0:4}-${date_after:5:2}.nc
  eratimestack=era-interim-tcwv-u10-v10-$year-$month-${date_after:0:4}-${date_after:5:2}.nc
  echo "START cdo mergetime - wallclock time is: `date`"
  cdo -b 32 mergetime $erathisday $eradayafter $eratimestack
  echo "END cdo mergetime - wallclock time is: `date`"
else
  eratimestack=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/$year/era-interim-t2m-mslp-tcwv-u10-v10-$year-$month.nc
fi

# interpolate temporally
echo "$(date +%Y-%m-%dT%H:%M:%S -u) interpolate temporally ..."
eramodistimeslice=$tmpdir/era-interim-t2m-mslp-tcwv-u10-v10-$year$month$day$hour$minute.nc
echo "START cdo mergetime - wallclock time is: `date`"
cdo inttime,$year-$month-$day,$hour:$minute:01 $eratimestack $eramodistimeslice
echo "END cdo inttime - wallclock time is: `date`"

# interpolate spatially
echo "$(date +%Y-%m-%dT%H:%M:%S -u) interpolate spatially ..."
eramodisspatial=$tmpdir/${modisstem}_era-interim_spatial.nc
echo "START cdo remapbil - wallclock time is: `date`"
cdo -L -f nc4c remapbil,$scripfile $eramodistimeslice $eramodisspatial
echo "END cdo remapbil - wallclock time is: `date`"

# remove cdo input...
sleep 10
rm -f $scripfile
rm -f $eramodistimeslice

# band subset: we need t2m, msl, tcwv, u10, v10
echo "START gpt band Subset - wallclock time is: `date`"
$SNAP_HOME/bin/gpt Subset -q 1 -Ssource=$eramodisspatial -PbandNames=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $eramodis
#$SNAP_HOME/bin/gpt Subset -q 4 -Ssource=$eramodisspatial -PbandNames=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $eramodis
#$SNAP_HOME/bin/gpt Subset -Ssource=$eramodisspatial -PbandNames=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $eramodis
echo "END gpt band Subset - wallclock time is: `date`"

# remove band subset input...
sleep 10
rm -f $eramodisspatial

# merge L1b with EraInterim band subset
l1bEraInterimMerge=$l1bEraInterimDir/${modisstem}_l1b-era-interim.nc
echo "START gpt Merge - wallclock time is: `date`"
$SNAP_HOME/bin/gpt ESACCI.MergeModisL1bEraInterim -q 1 -Sl1bProduct=$l1bPath -SeraInterimProduct=$eramodis -PeraInterimBandsToCopy=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $l1bEraInterimMerge
#$SNAP_HOME/bin/gpt ESACCI.MergeModisL1bEraInterim -q 4 -Sl1bProduct=$l1bPath -SeraInterimProduct=$eramodis -PeraInterimBandsToCopy=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $l1bEraInterimMerge
#$SNAP_HOME/bin/gpt ESACCI.MergeModisL1bEraInterim -Sl1bProduct=$l1bPath -SeraInterimProduct=$eramodis -PeraInterimBandsToCopy=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $l1bEraInterimMerge
echo "END gpt Merge - wallclock time is: `date`"

# remove EraInterim subset...
sleep 10
rm -f $eramodis

## TCWV
if [ -f $l1bEraInterimMerge ]; then
    #auxdataPath=/gws/nopw/j04/esacci_wv/software/dot_snap/auxdata/wvcci
    auxdataPath=/home/users/odanne/.snap/auxdata/wvcci
    tcwv=$tcwvDir/${modisstem}_tcwv.nc

    # If existing, add MOD35_L2 product as optional source product. 
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
	






