#!/bin/bash

idepixPath=$1
idepixBaseName=$2
tcwvL2Dir=$3
sensor=$4
year=$5
month=$6
wvcciRootDir=$7
snapRootDir=$8

tcwvFile=$tcwvL2Dir/${idepixBaseName}_tcwv.nc

if [ ! -e "$tcwvL2Dir" ]
then
    mkdir -p $tcwvL2Dir
fi

auxdataPath=/group_workspaces/cems2/qa4ecv/vol4/software/dot_snap/auxdata/wvcci

if [ "$sensor" == "MERIS" ]
then
    echo "time $snapRootDir/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PcloudFilterLevel=CLOUD_SURE_BUFFER -f NetCDF4-WVCCI -t $tcwvFile"
    time $snapRootDir/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PcloudFilterLevel=CLOUD_SURE_BUFFER -f NetCDF4-WVCCI -t $tcwvFile
elif [ "$sensor" == "MODIS_TERRA" ]
then
    echo "time $snapRootDir/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PcloudFilterLevel=CLOUD_SURE -f NetCDF4-WVCCI -t $tcwvFile"
    time $snapRootDir/bin/gpt ESACCI.Tcwv -e -SsourceProduct=$idepixPath -PauxdataPath=$auxdataPath -Psensor=$sensor -PcloudFilterLevel=CLOUD_SURE -f NetCDF4-WVCCI -t $tcwvFile
else
    echo "Invalid sensor $sensor - no Idepix processing started."
fi





status=$?
echo "Status: $status"

if [ $status = 0 ] && [ -e "$tcwvFile" ]
then
    echo "TCWV product created."
    status=$?
    echo "Status: $status"
else
    echo "No TCWV product created."
fi

echo `date`
