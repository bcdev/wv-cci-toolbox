#!/bin/bash


###################################################################
#this program is about creating brwose images frm albedo netcdf files
#author: Said Kharbouche, MSSL.UCL(2014)
####################################################################


###################################################### INPUTs #####################################################
###################################################################################################################

#list of inputs
SENSOR=$1
YEAR=$2
MONTH=$3
TCWVFILE=$4
OUTDIR=$5

if [ ! -f "$TCWVFILE" ]
then
    echo "Nc2browse input file '$TCWVFILE' does not exist - will exit."
    exit 1
fi

#scripts and colorlut directory
HOME=$GA_INST/bin/staging_said
###################################################################################################################

#if not existing, create output directory
mkdir -p $OUTDIR

SIZE05='720x360'
SIZE005='7200x3600'

COLORTXT='white'

PYTHON=$HOME/python/nctcwv2png.py
BAND=tcwv_mean_mean
MINMAX=0:70,0:70,0:70
#LUT=$HOME/params/color_lut_ga.txt
LUT='none'

echo -e "\n\n\n-------------------------------------------------------------"
echo python2.7 ${PYTHON} $SENSOR $YEAR $MONTH $TCWVFILE $OUTDIR $MINMAX $LUT $SIZE005 $COLORTXT $BAND
python2.7 ${PYTHON} $SENSOR $YEAR $MONTH $TCWVFILE $OUTDIR $MINMAX $LUT $SIZE005 $COLORTXT $BAND

status=$?
echo "Status: $status"

echo -e "\n\n\nDone."





