#!/bin/bash

l1bPath=$1
l1bBaseName=$2
idepixL2Dir=$3
year=$4
month=$5
wvcciRootDir=$6
snapRootDir=$7

idepixFile=$idepixL2Dir/${l1bBaseName}_idepix.nc

if [ ! -e "$idepixL2Dir" ]
then
    mkdir -p $idepixL2Dir
fi

#echo "time $snapRootDir/bin/gpt Snap.Idepix.Meris -e -c 8000M -q 24 -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-BEAM -t $idepixFile $l1bPath"
#time $snapRootDir/bin/gpt Snap.Idepix.Meris -e -c 8000M -q 24 -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-BEAM -t $idepixFile $l1bPath

echo "time $snapRootDir/bin/gpt Snap.Idepix.Meris -e -SsourceProduct=$l1bPath -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-IDEPIX-MERIS -t $idepixFile"
time $snapRootDir/bin/gpt Snap.Idepix.Meris -e -SsourceProduct=$l1bPath -PreflBandsToCopy=reflectance_13,reflectance_14,reflectance_15 -f NetCDF4-IDEPIX-MERIS -t $idepixFile

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
