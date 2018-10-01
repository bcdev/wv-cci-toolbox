#!/bin/bash

l1bPath=$1
l1bBaseName=$2
idepixL2Dir=$3
sensor=$4
year=$5
month=$6
wvcciRootDir=$7
snapRootDir=$8

idepixFile=$idepixL2Dir/${l1bBaseName}_idepix.nc

if [ ! -e "$idepixL2Dir" ]
then
    mkdir -p $idepixL2Dir
fi

if ["$sensor" == "MERIS"]
then
    echo "time $snapRootDir/bin/gpt Snap.Idepix.Meris -e -SsourceProduct=$l1bPath -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-IDEPIX-MERIS -t $idepixFile"
    time $snapRootDir/bin/gpt Snap.Idepix.Meris -e -SsourceProduct=$l1bPath -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-IDEPIX-MERIS -t $idepixFile
elif ["$sensor" == "MODIS"]
    echo "time $snapRootDir/bin/gpt Snap.Idepix.Modis -e -SsourceProduct=$l1bPath -PreflBandsToCopy=EV_250_Aggr1km_RefSB_2,EV_500_Aggr1km_RefSB_5,EV_1KM_RefSB_17,EV_1KM_RefSB_18,EV_1KM_RefSB_19 -f NetCDF4-IDEPIX-MODIS -t $idepixFile"
    time $snapRootDir/bin/gpt Snap.Idepix.Modis -e -SsourceProduct=$l1bPath -PreflBandsToCopy=EV_250_Aggr1km_RefSB_2,EV_500_Aggr1km_RefSB_5,EV_1KM_RefSB_17,EV_1KM_RefSB_18,EV_1KM_RefSB_19 -f NetCDF4-IDEPIX-MODIS -t $idepixFile
else
    echo "Invalid sensor $sensor - no Idepix processing started."
fi

status=$?
echo "Status: $status"

if [ $status = 0 ] && [ -e "$idepixFile" ]
then
    echo "Idepix product created."
    status=$?
    echo "Status: $status"
else
    echo "No Idepix product created."
fi

echo `date`
