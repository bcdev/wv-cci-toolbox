#!/bin/bash

l1bPath=$1
landMaskPath=$2
l1bFile=$3
idepixL2Dir=$4
sensor=$5
year=$6
month=$7
wvcciRootDir=$8
snapRootDir=$9

if [ "$sensor" == "MERIS" ]
then
    l1bBaseName=`basename $l1bFile .N1`
elif [ "$sensor" == "MODIS_TERRA" ]
then
    l1bBaseName=`basename $l1bFile .hdf`
else
    echo "Invalid sensor $sensor - no Idepix processing started."
fi

echo "l1bBaseName: $l1bBaseName"
idepixFile=$idepixL2Dir/${l1bBaseName}_idepix.nc

if [ ! -e "$idepixL2Dir" ]
then
    mkdir -p $idepixL2Dir
fi

if [ "$sensor" == "MERIS" ]
then
    echo "time $snapRootDir/bin/gpt Snap.Idepix.Meris -e -SsourceProduct=$l1bPath -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-IDEPIX-MERIS -t $idepixFile"
    #echo "time ${SNAP_HOME}/bin/gpt Idepix.Meris -e -SsourceProduct=$l1bPath -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-CF -t $idepixFile"
    time $snapRootDir/bin/gpt Snap.Idepix.Meris -e -SsourceProduct=$l1bPath -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-IDEPIX-MERIS -t $idepixFile
    #time ${SNAP_HOME}/bin/gpt Idepix.Meris -e -SsourceProduct=$l1bPath -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-CF -t $idepixFile
elif [ "$sensor" == "MODIS_TERRA" ]
then
    echo "time $snapRootDir/bin/gpt Snap.Idepix.Modis -e -SsourceProduct=$l1bPath -SmodisWaterMask=$landMaskPath -PreflBandsToCopy=EV_250_Aggr1km_RefSB_2,EV_500_Aggr1km_RefSB_5,EV_1KM_RefSB_17,EV_1KM_RefSB_18,EV_1KM_RefSB_19 -f NetCDF4-IDEPIX-MODIS -t $idepixFile"
    #echo "time ${SNAP_HOME}/bin/gpt Idepix.Modis -e -SsourceProduct=$l1bPath -SmodisWaterMask=$landMaskPath -PreflBandsToCopy=EV_250_Aggr1km_RefSB_2,EV_500_Aggr1km_RefSB_5,EV_1KM_RefSB_17,EV_1KM_RefSB_18,EV_1KM_RefSB_19 -f NetCDF4-CF -t $idepixFile"
    time $snapRootDir/bin/gpt Snap.Idepix.Modis -e -SsourceProduct=$l1bPath -SmodisWaterMask=$landMaskPath -PreflBandsToCopy=EV_250_Aggr1km_RefSB_2,EV_500_Aggr1km_RefSB_5,EV_1KM_RefSB_17,EV_1KM_RefSB_18,EV_1KM_RefSB_19 -f NetCDF4-IDEPIX-MODIS -t $idepixFile
    #time ${SNAP_HOME}/bin/gpt Idepix.Modis -e -SsourceProduct=$l1bPath -SmodisWaterMask=$landMaskPath -PreflBandsToCopy=EV_250_Aggr1km_RefSB_2,EV_500_Aggr1km_RefSB_5,EV_1KM_RefSB_17,EV_1KM_RefSB_18,EV_1KM_RefSB_19 -f NetCDF4-CF -t $idepixFile
else
    echo "Invalid sensor $sensor - no Idepix processing started."
fi

status=$?
echo "Status: $status"

if [ $status = 0 ] && [ -e "$idepixFile" ]
then
    echo "Idepix product created."
    echo "Status: $status"
else
    echo "Idepix product NOT successfully created (corrupt or not a Day product)."
    echo "Status: $status"
    if [ -e "$idepixFile" ]
    then
      echo "rm -f $idepixFile"
      rm -f $idepixFile   # delete corrupt file
    fi
fi

echo `date`
