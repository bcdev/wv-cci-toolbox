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
year=$4
month=$5
day=$6
wvcciRootDir=$7

tmpdir=$wvcciRootDir/tmp
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
  if ! $SNAP_HOME/bin/gpt Write -PformatName=SCRIP -Pfile=$scripfile $l1bPath
  then
    cat gpt.out
    exit 1
  fi
fi

# distinguish beginning of month, end of month, or in between and create time stacks

if [ "$day" = "01" -a "$year$month" != "197901" ]
then
  echo "$(date +%Y-%m-%dT%H:%M:%S -u) merge era stack with previous month ..."
  eradaybefore=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/${date_before:0:4}/era-interim-t2m-mslp-tcwv-u10-v10-${date_before:0:4}-${date_before:5:2}.nc
  erathisday=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/$year/era-interim-t2m-mslp-tcwv-u10-v10-$year-$month.nc
  eratimestack=era-interim-t2m-mslp-tcwv-u10-v10-${date_before:0:4}-${date_before:5:2}-$year-$month.nc
  cdo -b 32 mergetime $eradaybefore $erathisday $eratimestack
elif [ "$day" = "31" -a "$year$month" != "201509" ]
then
  echo "$(date +%Y-%m-%dT%H:%M:%S -u) merge era stack with next month ..."
  erathisday=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/$year/era-interim-t2m-mslp-tcwv-u10-v10-$year-$month.nc
  eradayafter=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/${date_after:0:4}/era-interim-t2m-mslp-tcwv-u10-v10-${date_after:0:4}-${date_after:5:2}.nc
  eratimestack=era-interim-tcwv-u10-v10-$year-$month-${date_after:0:4}-${date_after:5:2}.nc
  cdo -b 32 mergetime $erathisday $eradayafter $eratimestack
else
  eratimestack=$auxroot/era-interim-t2m-mslp-tcwv-u10-v10/$year/era-interim-t2m-mslp-tcwv-u10-v10-$year-$month.nc
fi

# interpolate temporally
echo "$(date +%Y-%m-%dT%H:%M:%S -u) interpolate temporally ..."
eramodistimeslice=$tmpdir/era-interim-t2m-mslp-tcwv-u10-v10-$year$month$day$hour$minute.nc
cdo inttime,$year-$month-$day,$hour:$minute:01 $eratimestack $eramodistimeslice

# interpolate spatially
echo "$(date +%Y-%m-%dT%H:%M:%S -u) interpolate spatially ..."
eramodisspatial=$tmpdir/${modisstem}_era-interim_spatial.nc
cdo -L -f nc4c remapbil,$scripfile $eramodistimeslice $eramodisspatial

# remove cdo input...
sleep 10
rm -f $scripfile
rm -f $eramodistimeslice

# band subset: we need t2m, msl, tcwv, u10, v10
$SNAP_HOME/bin/gpt Subset -q 1 -Ssource=$eramodisspatial -PbandNames=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $eramodis

# remove band subset input...
sleep 10
rm -f $eramodisspatial

# merge L1b with EraInterim band subset
l1bEraInterimMerge=$l1bEraInterimDir/${modisstem}_l1b-era-interim.nc
$SNAP_HOME/bin/gpt ESACCI.MergeModisL1bEraInterim -q 1 -Sl1bProduct=$l1bPath -SeraInterimProduct=$eramodis -PeraInterimBandsToCopy=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $l1bEraInterimMerge

# remove EraInterim subset...
sleep 10
rm -f $eramodis

## TCWV
if [ -f $l1bEraInterimMerge ]; then
    auxdataPath=/gws/nopw/j04/esacci_wv/software/dot_snap/auxdata/wvcci
    tcwv=$tcwvDir/${modisstem}_tcwv.nc

    # If existing, add MOD35_L2 product as optional source product. 
    # We want to write TCWV with NetCDF4_WVCCI in order NOT to write lat,lon!
    if [ -f $cloudMaskPath ]; then
      echo "${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 1-e -SsourceProduct=$l1bEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv"
      ${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 1 -e -SsourceProduct=$l1bEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv
    else
      echo "${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 1 -e -SsourceProduct=$l1bEraInterimMerge -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv"
      ${SNAP_HOME}/bin/gpt ESACCI.Tcwv -q 1-e -SsourceProduct=$l1bEraInterimMerge -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv
    fi
    status=$?
    echo "Status: $status"
fi

# cleanup: remove l1b-erainterim merge product (TCWV input)
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

echo `date`







