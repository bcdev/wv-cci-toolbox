#!/bin/bash
set -x
set -e

####################################################################
# Script for collocation of EraInterim auxdata product with MODIS Idepix.
# Follows original scripts 
# - era-interim-<sensor>-prepare
# - era-interim-<sensor>-process
# built on Calvalus by MB

####################################################################


###############################
# was PREPARE step on Calvalus: 
###############################

idepixPath=$1
idepixName=$2            # MOD021KM.A2008016.1540.006.2012066182519_idepix.nc
cloudMaskPath=$3         # cloud product e.g. MOD35_L2.A2008016.1540.006.2012066182519.hdf
year=$4
month=$5
day=$6
wvcciRootDir=$7

# initial check: does Idepix input exist at all? Maybe it was corrupt and deleted earlier. 
if [ ! -f $idepixPath ]; then
  echo "Idepix product '$idepixPath' does not exist - no action - will exit (0)."
  exit 0
fi

tmpdir=$wvcciRootDir/tmp
mkdir -p $tmpdir

#modisstem=${idepixName%.hdf}
modisstem=${idepixName%_idepix.nc}
hour=${idepixName:18:2}
minute=${idepixName:20:2}
date_in_seconds=$(($(date +%s -u -d "$year-01-01 $hour:$minute:00") + ( 1$doy - 1000 ) * 86400 - 86400))
month=$(date --date "@$date_in_seconds" +%m)
day=$(date --date "@$date_in_seconds" +%d)

acquisitionTime=$year$month$day

## replace 'L1b' by 'EraInterim' and '.hdf' by '_era-interim.nc':
# replace 'Idepix' by 'IdepixEraInterim' and '_idepix.nc' by '_era-interim.nc':
idepixDir=`dirname $idepixPath`
current="Idepix"
replace="IdepixEraInterim"
idepixEraInterimDir=${idepixDir/$current/$replace}
mkdir -p $idepixEraInterimDir

replace="Tcwv"
tcwvDir=${idepixDir/$current/$replace}
mkdir -p $tcwvDir

#eramodis=${eraInterimDir}/${modisstem}_era-interim.nc
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

echo "$(date +%Y-%m-%dT%H:%M:%S -u) extracting geo information of $idepixName ..."

scripfile=$tmpdir/${modisstem}-scrip.nc
if [ ! -e $scripfile ]; then
  #if ! $BEAM_HOME/bin/gpt.sh Write -PformatName=SCRIP -Pfile=$scripfile $idepixPath
  # TODO: replace by SNAP, test:
  if ! $SNAP_HOME/bin/gpt Write -PformatName=SCRIP -Pfile=$scripfile $idepixPath
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

# band subset: we need t2m, msl, tcwv, u10, v10
#$BEAM_HOME/bin/gpt.sh Subset -Ssource=$eramodisspatial -PbandNames=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $eramodis
# TODO: replace by SNAP, test:
$SNAP_HOME/bin/gpt Subset -Ssource=$eramodisspatial -PbandNames=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $eramodis


# merge Idepix with EraInterim band subset
idepixEraInterimMerge=$idepixEraInterimDir/${modisstem}_idepix-era-interim.nc
$SNAP_HOME/bin/gpt ESACCI.MergeIdepixEraInterim -SidepixProduct=$idepixPath -SeraInterimProduct=$eramodis -Psensor=MODIS_TERRA -PeraInterimBandsToCopy=t2m,msl,tcwv,u10,v10 -f Netcdf4-BEAM -t $idepixEraInterimMerge


## TCWV
if [ -f $idepixEraInterimMerge ]; then
    auxdataPath=/gws/nopw/j04/esacci_wv/software/dot_snap/auxdata/wvcci
    tcwv=$tcwvDir/${modisstem}_tcwv.nc

    # if existing, add MOD35_L2 product as optional source product, like: 
    #if [ -f $cloudMaskPath ]; then
    #  echo "$SNAP_HOME/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-BEAM -t $tcwv"
    #  $SNAP_HOME/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-BEAM -t $tcwv
    #else
    #  echo "$SNAP_HOME/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixEraInterimMerge -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-BEAM -t $tcwv"
    #  $SNAP_HOME/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixEraInterimMerge -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-BEAM -t $tcwv
    #fi

    SNAP_TMP_DIR=/gws/nopw/j04/esacci_wv/software/snap
    #SNAP_TMP_DIR=/gws/nopw/j04/esacci_wv/software/snap/release_7.0
    # todo: this should become SNAP_HOME. Check compatibility for Idepix and TCWV processing. We want to write TCWV with NetCDF4_WVCCI in order NOT to write lat,lon!
    # todo: check and fix problem:
    # Caused by: java.lang.NullPointerException
    #    at org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfFlagCodingPart.writeFlagCoding(CfFlagCodingPart.java:71)
    # products e.g.:
    # MOD021KM.A2016015.2035.061.2017324200543_idepix-era-interim.nc
    # MOD35_L2.A2016015.2035.061.2017324204633.hdf
    # OD, 20190914

    if [ -f $cloudMaskPath ]; then
      echo "${SNAP_TMP_DIR}/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv"
      ${SNAP_TMP_DIR}/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixEraInterimMerge -Smod35Product=$cloudMaskPath -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv
    else
      echo "${SNAP_TMP_DIR}/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixEraInterimMerge -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv"
      ${SNAP_TMP_DIR}/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixEraInterimMerge -PauxdataPath=$auxdataPath -Psensor=MODIS_TERRA -PprocessOcean=true -f NetCDF4-WVCCI -t $tcwv
    fi

fi
## 

# cleanup: keep idepix-erainterim merge product, remove pure idepix and all temporal products
sleep 30
#rm -f $scripfile
#rm -f $eramodistimeslice
#rm -f $eramodisspatial
#rm -f $eramodis
if [ -f $idepixEraInterimMerge ]; then
  echo "DELETING Idepix product : $idepixPath"
#  rm -f $idepixPath
  
  # Idepix products are large. As examples, just keep IdepixEraInterim of Jan 15th and Jul 15th of each year:
  if [ "$day" != "15" ] || ([ "$month" != "01" ] && [ "$month" != "07" ]); then
    echo "DELETING IdepixEraInterim merge product : $idepixEraInterimMerge"
#    rm -f $idepixEraInterimMerge
  fi
fi

# list results
echo "IDEPIX ERAINTERIM MERGE OUTPUT PRODUCT: $idepixEraInterimMerge"








